"""Comprehensive paginated scraper for Qualifax.ie course data."""

import asyncio
import json
import re
from typing import List, Dict, Any, Optional
from urllib.parse import urljoin
import pandas as pd
from playwright.async_api import async_playwright, Page, Browser
from loguru import logger
from tqdm import tqdm

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from config import SCRAPING_CONFIG, RAW_DATA_DIR
from utils.helpers import (
    clean_text, create_course_record, 
    get_random_user_agent
)


class ComprehensiveQualifaxScraper:
    """Comprehensive scraper that extracts all courses from Qualifax via pagination."""
    
    def __init__(self):
        self.base_url = "https://www.qualifax.ie"
        self.search_url = f"{self.base_url}/courses"
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        self.all_courses: List[Dict[str, Any]] = []
        self.max_pages = None
        
    async def __aenter__(self):
        """Async context manager entry."""
        playwright = await async_playwright().start()
        self.browser = await playwright.chromium.launch(
            headless=SCRAPING_CONFIG["headless"]
        )
        self.page = await self.browser.new_page()
        
        # Set user agent and longer timeout for slow pages
        await self.page.set_extra_http_headers({
            'User-Agent': get_random_user_agent()
        })
        
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        if self.browser:
            await self.browser.close()
    
    async def discover_total_pages(self) -> int:
        """Discover the total number of pages in the search results."""
        try:
            logger.info("Discovering total number of pages...")
            
            # Go to search page
            await self.page.goto(self.search_url, timeout=60000)
            await self.page.wait_for_load_state("networkidle")
            
            # Look for pagination elements
            pagination_selectors = [
                '.pagination a',
                '.pager a', 
                'a[href*="page="]',
                '.page-link',
                '.page-item a'
            ]
            
            max_page = 0
            
            for selector in pagination_selectors:
                try:
                    links = await self.page.query_selector_all(selector)
                    for link in links:
                        href = await link.get_attribute('href')
                        text = await link.text_content()
                        
                        if href:
                            # Look for page numbers in href
                            page_match = re.search(r'page=(\d+)', href)
                            if page_match:
                                page_num = int(page_match.group(1))
                                max_page = max(max_page, page_num)
                        
                        if text and text.isdigit():
                            page_num = int(text)
                            max_page = max(max_page, page_num)
                            
                except Exception as e:
                    logger.debug(f"Error checking pagination selector {selector}: {e}")
            
            # Also check the page content for hints about total pages
            page_content = await self.page.content()
            
            # Look for text patterns like "Page X of Y" or "Showing X-Y of Z"
            patterns = [
                r'page\s+\d+\s+of\s+(\d+)',
                r'showing\s+\d+\s*-\s*\d+\s+of\s+(\d+)',
                r'(\d+)\s+pages?',
                r'last.*?page.*?(\d+)'
            ]
            
            for pattern in patterns:
                matches = re.findall(pattern, page_content.lower())
                if matches:
                    try:
                        potential_max = max([int(m) for m in matches])
                        max_page = max(max_page, potential_max)
                    except:
                        continue
            
            # For /courses endpoint, we know there are 10,656 total courses
            # Force a higher page count to ensure we get everything
            if max_page < 200:
                logger.info("Setting higher page count to ensure we capture all 10,656 courses: 250 pages")
                max_page = 250
            
            logger.info(f"Discovered approximately {max_page} pages of results")
            return max_page
            
        except Exception as e:
            logger.error(f"Error discovering total pages: {e}")
            return 100  # Conservative fallback
    
    async def scrape_page(self, page_num: int) -> List[Dict[str, Any]]:
        """Scrape courses from a specific page."""
        courses = []
        
        try:
            # Construct page URL - use correct pagination structure for /courses endpoint
            page_url = f"{self.search_url}?items_per_page=100&page={page_num}"
            
            logger.debug(f"Scraping page {page_num}: {page_url}")
            
            # Navigate to page
            await self.page.goto(page_url, timeout=60000)
            await self.page.wait_for_load_state("networkidle", timeout=15000)
            
            # Look for course elements
            course_selectors = [
                '.search-result',
                '.course-item', 
                '.result-item',
                'article',
                'a[href*="/course/"]',
                'a[href*="/programme/"]',
                '.course-link',
                'h2 a, h3 a, h4 a'  # Course title links
            ]
            
            course_elements = []
            for selector in course_selectors:
                try:
                    elements = await self.page.query_selector_all(selector)
                    if elements:
                        course_elements.extend(elements)
                        logger.debug(f"Found {len(elements)} elements with selector: {selector}")
                except:
                    continue
            
            # Remove duplicates while preserving order
            seen_elements = set()
            unique_elements = []
            for element in course_elements:
                element_id = id(element)
                if element_id not in seen_elements:
                    seen_elements.add(element_id)
                    unique_elements.append(element)
            
            logger.debug(f"Page {page_num}: Processing {len(unique_elements)} unique course elements")
            
            # Extract course data from each element
            for element in unique_elements:
                try:
                    course_data = await self.extract_course_data(element)
                    if course_data and course_data.get('name'):
                        # Add page number for tracking
                        course_data['source_page'] = page_num
                        courses.append(course_data)
                except Exception as e:
                    logger.debug(f"Error extracting course data: {e}")
                    continue
            
            logger.debug(f"Page {page_num}: Extracted {len(courses)} valid courses")
            
            # Small delay to be respectful
            await asyncio.sleep(1)
            
        except Exception as e:
            logger.error(f"Error scraping page {page_num}: {e}")
        
        return courses
    
    async def extract_course_data(self, element) -> Optional[Dict[str, Any]]:
        """Extract course data from a course element."""
        try:
            # Try to get course name
            name = ""
            
            # Try different approaches to get the course name
            name_selectors = ['h1', 'h2', 'h3', 'h4', '.title', '.name', '.course-title']
            
            # First try to find title elements within this element
            for selector in name_selectors:
                try:
                    title_element = await element.query_selector(selector)
                    if title_element:
                        name_text = await title_element.text_content()
                        if name_text and len(name_text.strip()) > 3:
                            name = name_text
                            break
                except:
                    continue
            
            # If no title found, check if this element itself is a link with text
            if not name:
                try:
                    name = await element.text_content()
                    if not name or len(name.strip()) < 3:
                        return None
                except:
                    return None
            
            name = clean_text(name)
            
            if not name or len(name) < 3:
                return None
            
            # Try to get course URL
            course_url = ""
            try:
                # Check if this element is a link
                href = await element.get_attribute('href')
                if href:
                    course_url = urljoin(self.base_url, href)
                else:
                    # Look for links within the element
                    link = await element.query_selector('a')
                    if link:
                        href = await link.get_attribute('href')
                        if href:
                            course_url = urljoin(self.base_url, href)
            except:
                pass
            
            # Try to get description
            description = ""
            desc_selectors = ['.description', '.summary', '.excerpt', 'p', '.content']
            for selector in desc_selectors:
                try:
                    desc_element = await element.query_selector(selector)
                    if desc_element:
                        desc_text = await desc_element.text_content()
                        if desc_text and len(desc_text.strip()) > 20:
                            description = clean_text(desc_text)
                            break
                except:
                    continue
            
            # Try to extract course code from name or URL
            cao_code = ""
            import re
            
            # Look for course codes in name
            code_match = re.search(r'([A-Z]{2,4}\d{3,4})', name.upper())
            if code_match:
                cao_code = code_match.group(1)
            elif course_url:
                # Try to extract from URL
                code_match = re.search(r'([A-Z]{2,4}\d{3,4})', course_url.upper())
                if code_match:
                    cao_code = code_match.group(1)
            
            # Determine NFQ level
            nfq_level = 8  # Default
            content = f"{name} {description}".lower()
            
            if 'certificate' in content or 'cert' in content:
                nfq_level = 6
            elif 'diploma' in content:
                nfq_level = 7
            elif 'master' in content or 'msc' in content or 'ma ' in content:
                nfq_level = 9
            elif 'phd' in content or 'doctorate' in content:
                nfq_level = 10
            
            # Determine category
            tags = "General"
            if any(word in content for word in ['engineer', 'technology', 'computer', 'science', 'data', 'software']):
                tags = "STEM"
            elif any(word in content for word in ['business', 'management', 'accounting', 'finance', 'marketing']):
                tags = "Business"
            elif any(word in content for word in ['art', 'design', 'music', 'creative', 'media', 'film']):
                tags = "Arts"
            elif any(word in content for word in ['health', 'medicine', 'nursing', 'therapy', 'medical', 'care']):
                tags = "Health"
            elif any(word in content for word in ['education', 'teaching', 'early learning', 'childcare']):
                tags = "Education"
            
            return create_course_record(
                cao_code=cao_code,
                name=name,
                description=description,
                nfq_level=nfq_level,
                tags=tags,
                course_url=course_url
            )
            
        except Exception as e:
            logger.debug(f"Error extracting course data from element: {e}")
            return None
    
    async def run_comprehensive_scrape(self, max_pages: Optional[int] = None, sample_pages: Optional[int] = None) -> List[Dict[str, Any]]:
        """Run comprehensive scraping of all pages."""
        try:
            logger.info("Starting comprehensive Qualifax course scraping...")
            
            # Discover total pages
            if not max_pages:
                max_pages = await self.discover_total_pages()
            
            self.max_pages = max_pages
            
            # If sample_pages is specified, only scrape that many pages
            if sample_pages:
                pages_to_scrape = min(sample_pages, max_pages)
                logger.info(f"Sampling {pages_to_scrape} pages out of {max_pages} total pages")
            else:
                pages_to_scrape = max_pages
                logger.info(f"Scraping all {pages_to_scrape} pages")
            
            all_courses = []
            
            # Create progress bar
            with tqdm(total=pages_to_scrape, desc="Scraping pages") as pbar:
                # Scrape pages in batches to avoid overwhelming the server
                batch_size = 10
                
                for batch_start in range(0, pages_to_scrape, batch_size):
                    batch_end = min(batch_start + batch_size, pages_to_scrape)
                    
                    logger.info(f"Processing batch: pages {batch_start} to {batch_end-1}")
                    
                    # Process batch
                    for page_num in range(batch_start, batch_end):
                        try:
                            courses = await self.scrape_page(page_num)
                            all_courses.extend(courses)
                            pbar.update(1)
                            pbar.set_postfix({'courses': len(all_courses)})
                            
                        except Exception as e:
                            logger.error(f"Error scraping page {page_num}: {e}")
                            pbar.update(1)
                            continue
                    
                    # Small delay between batches
                    if batch_end < pages_to_scrape:
                        logger.info(f"Completed batch. Total courses so far: {len(all_courses)}")
                        await asyncio.sleep(3)  # Longer delay between batches
            
            # Remove duplicates based on course name and URL
            unique_courses = []
            seen = set()
            
            for course in all_courses:
                key = (course.get('name', ''), course.get('course_url', ''))
                if key not in seen:
                    seen.add(key)
                    unique_courses.append(course)
            
            duplicates_removed = len(all_courses) - len(unique_courses)
            logger.info(f"Removed {duplicates_removed} duplicate courses")
            
            self.all_courses = unique_courses
            
            logger.success(f"Comprehensive scraping completed! Extracted {len(unique_courses)} unique courses from {pages_to_scrape} pages")
            
            return unique_courses
            
        except Exception as e:
            logger.error(f"Error in comprehensive scraping: {e}")
            return []
    
    def save_data(self, filename: str = None):
        """Save scraped data."""
        if not self.all_courses:
            logger.warning("No courses to save")
            return
        
        if not filename:
            from datetime import datetime
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"qualifax_comprehensive_{timestamp}"
        
        # Save JSON
        json_path = RAW_DATA_DIR / f"{filename}.json"
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(self.all_courses, f, indent=2, ensure_ascii=False)
        
        # Save CSV
        csv_path = RAW_DATA_DIR / f"{filename}.csv"
        df = pd.DataFrame(self.all_courses)
        df.to_csv(csv_path, index=False, encoding='utf-8')
        
        logger.info(f"Saved {len(self.all_courses)} courses to:")
        logger.info(f"  JSON: {json_path}")
        logger.info(f"  CSV: {csv_path}")


async def main():
    """Test the comprehensive scraper."""
    async with ComprehensiveQualifaxScraper() as scraper:
        # Start with a sample to test
        courses = await scraper.run_comprehensive_scrape(sample_pages=50)  # Test with 50 pages first
        scraper.save_data("qualifax_sample_50pages")
        
        logger.info(f"Sample scrape completed: {len(courses)} courses")
        
        if courses:
            logger.info("Sample courses:")
            for i, course in enumerate(courses[:5]):
                logger.info(f"{i+1}. {course['name']} [{course.get('cao_code', 'No Code')}]")


if __name__ == "__main__":
    asyncio.run(main())