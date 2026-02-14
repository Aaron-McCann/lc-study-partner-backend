"""Enhanced scraper for university provider pages on Qualifax.ie to capture missing university courses."""

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


class UniversityProviderScraper:
    """Scraper specifically for university provider pages to capture missing courses."""
    
    def __init__(self):
        self.base_url = "https://www.qualifax.ie"
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        self.all_courses: List[Dict[str, Any]] = []
        
        # Major Irish universities to scrape
        self.universities = [
            "trinity-college-dublin",
            "university-college-dublin", 
            "university-college-cork",
            "university-of-galway",
            "university-of-limerick",
            "dublin-city-university",
            "maynooth-university",
            "technological-university-dublin",
            "atlantic-technological-university",
            "munster-technological-university",
            "south-east-technological-university"
        ]
        
    async def __aenter__(self):
        """Async context manager entry."""
        playwright = await async_playwright().start()
        self.browser = await playwright.chromium.launch(
            headless=SCRAPING_CONFIG["headless"]
        )
        self.page = await self.browser.new_page()
        
        # Set user agent
        await self.page.set_extra_http_headers({
            'User-Agent': get_random_user_agent()
        })
        
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        if self.browser:
            await self.browser.close()
    
    async def scrape_university_provider(self, university_slug: str) -> List[Dict[str, Any]]:
        """Scrape all courses from a specific university provider page."""
        courses = []
        university_name = university_slug.replace("-", " ").title()
        
        try:
            # Construct provider URL
            provider_url = f"{self.base_url}/providers/{university_slug}"
            
            logger.info(f"Scraping {university_name}: {provider_url}")
            
            # Navigate to provider page
            await self.page.goto(provider_url, timeout=60000)
            await self.page.wait_for_load_state("networkidle", timeout=15000)
            
            # Check if page exists - but be less strict with detection
            page_content = await self.page.content()
            if '404' in page_content or ('not found' in page_content.lower() and len(page_content) < 1000):
                logger.warning(f"Provider page not found for {university_name}")
                return []
            
            # Log page title for debugging
            page_title = await self.page.title()
            logger.debug(f"Page title for {university_name}: {page_title}")
            
            # Look for course count information
            course_count_text = await self.page.locator('text=/\\d+ courses?/').first.text_content()
            if course_count_text:
                course_count_match = re.search(r'(\d+)', course_count_text)
                if course_count_match:
                    total_courses = int(course_count_match.group(1))
                    logger.info(f"{university_name} has {total_courses} courses")
            
            # Look for course links
            course_selectors = [
                'a[href*="/course/"]',
                '.course-item a',
                '.course-link',
                'tbody tr a'  # Table format
            ]
            
            course_elements = []
            for selector in course_selectors:
                try:
                    elements = await self.page.query_selector_all(selector)
                    if elements:
                        course_elements.extend(elements)
                        logger.debug(f"Found {len(elements)} course elements with selector: {selector}")
                except:
                    continue
            
            # Handle pagination if it exists
            page_num = 1
            max_pages = 10  # Safety limit
            
            while page_num <= max_pages:
                # Extract courses from current page
                current_page_courses = await self.extract_courses_from_page(course_elements, university_name)
                courses.extend(current_page_courses)
                
                # Check for next page
                next_page_found = False
                try:
                    next_page_selectors = [
                        'a[rel="next"]',
                        '.pager-next a',
                        'a:has-text("Next")',
                        'a:has-text("›")'
                    ]
                    
                    for selector in next_page_selectors:
                        next_link = await self.page.query_selector(selector)
                        if next_link:
                            await next_link.click()
                            await self.page.wait_for_load_state("networkidle", timeout=10000)
                            
                            # Get new course elements
                            course_elements = []
                            for course_selector in course_selectors:
                                try:
                                    elements = await self.page.query_selector_all(course_selector)
                                    if elements:
                                        course_elements.extend(elements)
                                except:
                                    continue
                            
                            page_num += 1
                            next_page_found = True
                            logger.debug(f"Moved to page {page_num} for {university_name}")
                            break
                    
                    if not next_page_found:
                        break
                        
                except Exception as e:
                    logger.debug(f"No more pages for {university_name}: {e}")
                    break
            
            logger.info(f"Extracted {len(courses)} courses from {university_name}")
            
            # Small delay to be respectful
            await asyncio.sleep(2)
            
        except Exception as e:
            logger.error(f"Error scraping {university_name}: {e}")
        
        return courses
    
    async def extract_courses_from_page(self, course_elements: list, university_name: str) -> List[Dict[str, Any]]:
        """Extract course data from course elements on the current page."""
        courses = []
        
        # Remove duplicates while preserving order
        seen_elements = set()
        unique_elements = []
        for element in course_elements:
            element_id = id(element)
            if element_id not in seen_elements:
                seen_elements.add(element_id)
                unique_elements.append(element)
        
        for element in unique_elements:
            try:
                course_data = await self.extract_course_data(element, university_name)
                if course_data and course_data.get('name'):
                    courses.append(course_data)
            except Exception as e:
                logger.debug(f"Error extracting course data: {e}")
                continue
        
        return courses
    
    async def extract_course_data(self, element, university_name: str) -> Optional[Dict[str, Any]]:
        """Extract course data from a course element."""
        try:
            # Try to get course URL
            course_url = ""
            href = await element.get_attribute('href')
            if href:
                course_url = urljoin(self.base_url, href)
            
            # Try to get course name from the link text
            name = await element.text_content()
            if not name or len(name.strip()) < 3:
                return None
            
            name = clean_text(name)
            
            # Try to extract course code from name or URL
            cao_code = ""
            code_match = re.search(r'([A-Z]{2,4}\\d{3,4})', name.upper())
            if code_match:
                cao_code = code_match.group(1)
            elif course_url:
                # Try to extract from URL
                code_match = re.search(r'([A-Z]{2,4}\\d{3,4})', course_url.upper())
                if code_match:
                    cao_code = code_match.group(1)
            
            # Determine NFQ level from course name
            nfq_level = 8  # Default to Bachelor's
            content = name.lower()
            
            if 'certificate' in content or 'cert' in content:
                nfq_level = 6
            elif 'diploma' in content:
                nfq_level = 7
            elif any(word in content for word in ['master', 'msc', 'ma ', 'mba', 'med']):
                nfq_level = 9
            elif 'phd' in content or 'doctorate' in content:
                nfq_level = 10
            
            # Determine category based on course name
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
                description=f"Course offered by {university_name}",
                nfq_level=nfq_level,
                tags=tags,
                course_url=course_url,
                college_id=university_name
            )
            
        except Exception as e:
            logger.debug(f"Error extracting course data from element: {e}")
            return None
    
    async def run_university_scraping(self) -> List[Dict[str, Any]]:
        """Run comprehensive scraping of all major university provider pages."""
        logger.info("Starting comprehensive university provider scraping...")
        
        all_courses = []
        
        with tqdm(total=len(self.universities), desc="Scraping universities") as pbar:
            for university in self.universities:
                try:
                    courses = await self.scrape_university_provider(university)
                    all_courses.extend(courses)
                    pbar.update(1)
                    pbar.set_postfix({'total_courses': len(all_courses)})
                except Exception as e:
                    logger.error(f"Error scraping university {university}: {e}")
                    pbar.update(1)
                    continue
        
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
        
        logger.success(f"University scraping completed! Extracted {len(unique_courses)} unique university courses")
        
        return unique_courses
    
    def save_data(self, filename: str = None):
        """Save scraped data."""
        if not self.all_courses:
            logger.warning("No courses to save")
            return
        
        if not filename:
            from datetime import datetime
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"qualifax_universities_{timestamp}"
        
        # Save JSON
        json_path = RAW_DATA_DIR / f"{filename}.json"
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(self.all_courses, f, indent=2, ensure_ascii=False)
        
        # Save CSV
        csv_path = RAW_DATA_DIR / f"{filename}.csv"
        df = pd.DataFrame(self.all_courses)
        df.to_csv(csv_path, index=False, encoding='utf-8')
        
        logger.info(f"Saved {len(self.all_courses)} university courses to:")
        logger.info(f"  JSON: {json_path}")
        logger.info(f"  CSV: {csv_path}")


async def main():
    """Test the university provider scraper."""
    async with UniversityProviderScraper() as scraper:
        courses = await scraper.run_university_scraping()
        scraper.save_data("qualifax_universities_comprehensive")
        
        logger.info(f"University scraping completed: {len(courses)} courses")
        
        if courses:
            logger.info("Sample university courses:")
            for i, course in enumerate(courses[:10]):
                logger.info(f"{i+1}. {course['name']} [{course.get('college_id', 'Unknown')}]")


if __name__ == "__main__":
    asyncio.run(main())