"""Scraper for Qualifax.ie course data."""

import asyncio
import json
from typing import List, Dict, Any, Optional
from urllib.parse import urljoin, urlparse
import pandas as pd
from playwright.async_api import async_playwright, Page, Browser
from loguru import logger
from tqdm import tqdm

import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from config import SCRAPING_CONFIG, SOURCES, RAW_DATA_DIR
from utils.helpers import (
    respectful_delay, clean_text, create_course_record, 
    extract_year_from_text, get_random_user_agent
)


class QualifaxScraper:
    """Scraper for Qualifax.ie course database."""
    
    def __init__(self):
        self.base_url = SOURCES["qualifax"]["base_url"]
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        self.scraped_courses: List[Dict[str, Any]] = []
        
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
    
    async def scrape_course_search_page(self) -> List[str]:
        """Find course search functionality and get course listing URLs."""
        try:
            # Start from the main page
            await self.page.goto(self.base_url, timeout=SCRAPING_CONFIG["timeout"] * 1000)
            await self.page.wait_for_load_state("networkidle")
            
            logger.info("Exploring Qualifax course search functionality...")
            
            # Look for course-related links
            course_links = []
            
            # Try to find course search or course listing links
            possible_selectors = [
                'a[href*="course"]',
                'a[href*="search"]',
                'a[href*="browse"]',
                'a[href*="degree"]',
                'a[href*="programme"]',
                '.navigation a',
                '.menu a',
                'nav a'
            ]
            
            for selector in possible_selectors:
                try:
                    elements = await self.page.query_selector_all(selector)
                    for element in elements:
                        href = await element.get_attribute('href')
                        text = await element.text_content()
                        
                        if href and text:
                            text_lower = text.lower()
                            if any(keyword in text_lower for keyword in ['course', 'search', 'browse', 'degree', 'programme']):
                                full_url = urljoin(self.base_url, href)
                                course_links.append(full_url)
                                logger.debug(f"Found course link: {text} -> {full_url}")
                except Exception as e:
                    logger.debug(f"Error with selector {selector}: {e}")
                    continue
            
            # Remove duplicates
            course_links = list(set(course_links))
            logger.info(f"Found {len(course_links)} potential course-related URLs")
            
            return course_links
            
        except Exception as e:
            logger.error(f"Error exploring course search page: {e}")
            return []
    
    async def scrape_course_listing(self, url: str) -> List[Dict[str, Any]]:
        """Scrape courses from a course listing page."""
        courses = []
        
        try:
            logger.info(f"Scraping course listing: {url}")
            await self.page.goto(url, timeout=SCRAPING_CONFIG["timeout"] * 1000)
            await self.page.wait_for_load_state("networkidle")
            
            # Look for course cards, listings, or table rows
            course_selectors = [
                '.course-item',
                '.course-card', 
                '.course-listing',
                '.search-result',
                'tr:has(a)',  # Table rows with links
                '.result-item',
                'article',
                '.entry'
            ]
            
            course_elements = []
            for selector in course_selectors:
                try:
                    elements = await self.page.query_selector_all(selector)
                    if elements:
                        course_elements.extend(elements)
                        logger.debug(f"Found {len(elements)} elements with selector: {selector}")
                except Exception as e:
                    logger.debug(f"Error with course selector {selector}: {e}")
            
            if not course_elements:
                # Fallback: look for any links that might be courses
                course_elements = await self.page.query_selector_all('a[href*="course"], a[href*="programme"], a[href*="degree"]')
            
            logger.info(f"Found {len(course_elements)} potential course elements")
            
            for element in course_elements[:100]:  # Get more courses
                try:
                    course_data = await self.extract_course_data(element)
                    if course_data and course_data.get('name'):
                        courses.append(course_data)
                        logger.debug(f"Extracted course: {course_data.get('name', 'Unknown')}")
                except Exception as e:
                    logger.debug(f"Error extracting course data: {e}")
                    continue
        
        except Exception as e:
            logger.error(f"Error scraping course listing {url}: {e}")
        
        return courses
    
    async def extract_course_data(self, element) -> Optional[Dict[str, Any]]:
        """Extract course data from a course element."""
        try:
            # Try to get course name
            name = ""
            name_selectors = ['h1', 'h2', 'h3', '.title', '.name', '.course-title', 'a']
            for selector in name_selectors:
                try:
                    name_element = await element.query_selector(selector)
                    if name_element:
                        name = await name_element.text_content()
                        if name and len(name.strip()) > 3:
                            break
                except:
                    continue
            
            if not name or len(name.strip()) < 3:
                return None
            
            name = clean_text(name)
            
            # Try to get course URL
            course_url = ""
            try:
                link = await element.query_selector('a')
                if link:
                    href = await link.get_attribute('href')
                    if href:
                        course_url = urljoin(self.base_url, href)
            except:
                pass
            
            # Try to get description
            description = ""
            desc_selectors = ['.description', '.summary', '.excerpt', 'p']
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
            code_match = re.search(r'([A-Z]{2,4}\d{3,4})', name)
            if code_match:
                cao_code = code_match.group(1)
            elif course_url:
                # Try to extract from URL
                code_match = re.search(r'([A-Z]{2,4}\d{3,4})', course_url)
                if code_match:
                    cao_code = code_match.group(1)
            
            # Determine NFQ level (default to 8 for degrees)
            nfq_level = 8
            name_lower = name.lower()
            if 'certificate' in name_lower or 'cert' in name_lower:
                nfq_level = 6
            elif 'diploma' in name_lower:
                nfq_level = 7
            elif 'master' in name_lower or 'msc' in name_lower or 'ma ' in name_lower:
                nfq_level = 9
            elif 'phd' in name_lower or 'doctorate' in name_lower:
                nfq_level = 10
            
            # Try to determine tags/category
            tags = "General"
            if any(word in name_lower for word in ['engineer', 'technology', 'computer', 'science']):
                tags = "STEM"
            elif any(word in name_lower for word in ['business', 'management', 'accounting']):
                tags = "Business"
            elif any(word in name_lower for word in ['art', 'design', 'music', 'creative']):
                tags = "Arts"
            elif any(word in name_lower for word in ['health', 'medicine', 'nursing']):
                tags = "Health"
            
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
    
    async def run_full_scrape(self) -> List[Dict[str, Any]]:
        """Run the complete Qualifax scraping process."""
        try:
            logger.info("Starting Qualifax course data scraping...")
            
            # First, find course search/listing pages
            course_urls = await self.scrape_course_search_page()
            
            if not course_urls:
                logger.warning("No course listing URLs found, trying direct search...")
                # Try some common course listing paths
                fallback_urls = [
                    f"{self.base_url}/courses",
                    f"{self.base_url}/search",
                    f"{self.base_url}/course-search",
                    f"{self.base_url}/browse-courses"
                ]
                course_urls = fallback_urls
            
            all_courses = []
            
            # Scrape each course listing page
            for url in course_urls[:5]:  # Get more comprehensive data
                try:
                    await asyncio.sleep(2)  # Respectful delay
                    courses = await self.scrape_course_listing(url)
                    all_courses.extend(courses)
                    logger.info(f"Scraped {len(courses)} courses from {url}")
                    
                except Exception as e:
                    logger.error(f"Error scraping {url}: {e}")
                    continue
            
            # Remove duplicates based on course name
            seen_names = set()
            unique_courses = []
            for course in all_courses:
                name = course.get('name', '').lower()
                if name and name not in seen_names:
                    seen_names.add(name)
                    unique_courses.append(course)
            
            self.scraped_courses = unique_courses
            logger.info(f"Scraped {len(unique_courses)} unique courses from Qualifax")
            
            return unique_courses
            
        except Exception as e:
            logger.error(f"Error in full Qualifax scrape: {e}")
            return []
    
    def save_raw_data(self, filename: str = "qualifax_courses_raw.json"):
        """Save scraped data to raw data directory."""
        if not self.scraped_courses:
            logger.warning("No courses to save")
            return
        
        filepath = RAW_DATA_DIR / filename
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(self.scraped_courses, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Saved {len(self.scraped_courses)} courses to {filepath}")
            
            # Also save as CSV for easy viewing
            if self.scraped_courses:
                df = pd.DataFrame(self.scraped_courses)
                csv_filepath = RAW_DATA_DIR / filename.replace('.json', '.csv')
                df.to_csv(csv_filepath, index=False, encoding='utf-8')
                logger.info(f"Also saved CSV to {csv_filepath}")
                
        except Exception as e:
            logger.error(f"Error saving raw data: {e}")


async def main():
    """Main function for testing the Qualifax scraper."""
    async with QualifaxScraper() as scraper:
        courses = await scraper.run_full_scrape()
        scraper.save_raw_data()
        
        if courses:
            logger.info(f"Successfully scraped {len(courses)} courses")
            for course in courses[:5]:  # Show first 5 courses
                logger.info(f"Course: {course.get('name')} - {course.get('cao_code')}")
        else:
            logger.warning("No courses were scraped")


if __name__ == "__main__":
    asyncio.run(main())