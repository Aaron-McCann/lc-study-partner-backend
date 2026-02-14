"""Ultra-careful scraper designed to avoid IP bans with human-like behavior."""

import asyncio
import json
import re
import random
from typing import List, Dict, Any, Optional
from urllib.parse import urljoin
import pandas as pd
from playwright.async_api import async_playwright, Page, Browser
from loguru import logger
from tqdm import tqdm
import time
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from config import SCRAPING_CONFIG, RAW_DATA_DIR
from utils.helpers import (
    clean_text, create_course_record, 
    get_random_user_agent
)


class StealthScraper:
    """Ultra-careful scraper with human-like behavior to avoid bans."""
    
    def __init__(self):
        self.base_url = "https://www.qualifax.ie"
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        self.all_courses: List[Dict[str, Any]] = []
        self.session_requests = 0
        self.last_request_time = 0
        
        # Very conservative rate limiting
        self.min_delay = 10  # Minimum 10 seconds between requests
        self.max_delay = 30  # Maximum 30 seconds between requests
        self.requests_per_hour = 60  # Maximum 60 requests per hour
        
    async def __aenter__(self):
        """Async context manager entry with stealth configuration."""
        playwright = await async_playwright().start()
        
        # Launch browser with stealth settings
        self.browser = await playwright.chromium.launch(
            headless=True,  # Always headless for stealth
            args=[
                '--no-sandbox',
                '--disable-blink-features=AutomationControlled',
                '--disable-web-security',
                '--disable-features=VizDisplayCompositor',
                '--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            ]
        )
        
        # Create new context with realistic settings
        context = await self.browser.new_context(
            viewport={'width': 1920, 'height': 1080},
            user_agent='Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            extra_http_headers={
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
                'Accept-Language': 'en-US,en;q=0.5',
                'Accept-Encoding': 'gzip, deflate',
                'DNT': '1',
                'Connection': 'keep-alive'
            }
        )
        
        self.page = await context.new_page()
        
        # Remove webdriver traces
        await self.page.add_init_script("""
            Object.defineProperty(navigator, 'webdriver', {
                get: () => undefined,
            });
        """)
        
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        if self.browser:
            await self.browser.close()
    
    async def human_like_delay(self):
        """Implement human-like delays between requests."""
        self.session_requests += 1
        
        # Enforce hourly rate limit
        if self.session_requests >= self.requests_per_hour:
            logger.warning("Hourly rate limit reached, pausing for 1 hour...")
            await asyncio.sleep(3600)  # Wait 1 hour
            self.session_requests = 0
        
        # Random delay between min and max
        delay = random.uniform(self.min_delay, self.max_delay)
        
        # Add extra delay every 10 requests (like a human taking a break)
        if self.session_requests % 10 == 0:
            extra_delay = random.uniform(60, 180)  # 1-3 minute break
            logger.info(f"Taking a break for {extra_delay:.1f} seconds...")
            await asyncio.sleep(extra_delay)
        
        logger.debug(f"Waiting {delay:.1f} seconds before next request...")
        await asyncio.sleep(delay)
        self.last_request_time = time.time()
    
    async def navigate_carefully(self, url: str) -> bool:
        """Navigate to URL with careful error handling."""
        try:
            await self.human_like_delay()
            
            # Navigate with realistic timeout
            response = await self.page.goto(url, 
                timeout=60000,  # 1 minute timeout
                wait_until="networkidle"
            )
            
            if response and response.status == 403:
                logger.error("403 Forbidden - IP might be banned")
                return False
            elif response and response.status == 429:
                logger.warning("Rate limited, waiting longer...")
                await asyncio.sleep(300)  # Wait 5 minutes
                return False
            elif response and response.status >= 400:
                logger.warning(f"HTTP {response.status} for {url}")
                return False
            
            # Random mouse movement to appear human
            await self.simulate_human_behavior()
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to navigate to {url}: {e}")
            return False
    
    async def simulate_human_behavior(self):
        """Simulate human-like browser behavior."""
        try:
            # Random scroll
            await self.page.evaluate("window.scrollTo(0, Math.random() * 500)")
            await asyncio.sleep(random.uniform(0.5, 2))
            
            # Random mouse movement
            await self.page.mouse.move(
                random.randint(100, 800),
                random.randint(100, 600)
            )
            await asyncio.sleep(random.uniform(0.2, 1))
            
        except:
            pass  # Ignore if simulation fails
    
    async def scrape_trinity_carefully(self) -> List[Dict[str, Any]]:
        """Carefully scrape Trinity courses with maximum stealth."""
        logger.info("Starting careful Trinity scraping with anti-ban measures...")
        
        trinity_courses = []
        page_num = 1
        max_pages = 50  # Safety limit
        
        while page_num <= max_pages:
            # Construct Trinity provider URL with pagination
            trinity_url = f"{self.base_url}/providers/trinity-college-dublin"
            if page_num > 1:
                trinity_url += f"?page={page_num}"
            
            logger.info(f"Scraping Trinity page {page_num}: {trinity_url}")
            
            # Navigate carefully
            if not await self.navigate_carefully(trinity_url):
                logger.error(f"Failed to load Trinity page {page_num}")
                break
            
            # Check for 404 or ban
            page_content = await self.page.content()
            if '404' in page_content or 'forbidden' in page_content.lower():
                logger.warning(f"Trinity page {page_num} not found or forbidden")
                break
            
            # Check if this page has courses
            course_links = await self.page.query_selector_all('a[href*="/course/"]')
            if not course_links:
                logger.info(f"No course links found on Trinity page {page_num}")
                break
            
            logger.info(f"Found {len(course_links)} course links on page {page_num}")
            
            # Extract courses from this page
            page_courses = await self.extract_trinity_courses_from_page(course_links)
            trinity_courses.extend(page_courses)
            
            logger.info(f"Page {page_num}: Extracted {len(page_courses)} courses. Total: {len(trinity_courses)}")
            
            # Check for next page
            next_page_found = False
            try:
                next_selectors = ['a[rel="next"]', '.pager-next a', 'a:has-text("Next")', 'a:has-text("›")']
                for selector in next_selectors:
                    next_link = await self.page.query_selector(selector)
                    if next_link:
                        next_page_found = True
                        break
            except:
                pass
            
            if not next_page_found:
                logger.info(f"No next page found after page {page_num}")
                break
            
            page_num += 1
            
            # Extra delay between pages
            extra_delay = random.uniform(30, 60)
            logger.info(f"Completed page {page_num-1}, waiting {extra_delay:.1f}s before next page...")
            await asyncio.sleep(extra_delay)
        
        logger.success(f"Trinity scraping completed! Found {len(trinity_courses)} courses across {page_num-1} pages")
        return trinity_courses
    
    async def extract_trinity_courses_from_page(self, course_elements: list) -> List[Dict[str, Any]]:
        """Extract course data from Trinity course elements."""
        courses = []
        
        for i, element in enumerate(course_elements):
            try:
                # Get course URL
                course_url = ""
                href = await element.get_attribute('href')
                if href:
                    course_url = urljoin(self.base_url, href)
                
                # Get course name
                name = await element.text_content()
                if not name or len(name.strip()) < 3:
                    continue
                
                name = clean_text(name)
                
                # Extract course code
                cao_code = ""
                code_match = re.search(r'([A-Z]{2,4}\d{3,4})', name.upper())
                if code_match:
                    cao_code = code_match.group(1)
                
                # Determine course level and category
                nfq_level = 8  # Default Bachelor's
                content = name.lower()
                
                if any(word in content for word in ['master', 'msc', 'ma ', 'mba', 'med']):
                    nfq_level = 9
                elif any(word in content for word in ['phd', 'doctorate', 'doctoral']):
                    nfq_level = 10
                elif 'diploma' in content:
                    nfq_level = 7
                elif 'certificate' in content:
                    nfq_level = 6
                
                # Determine category
                tags = "General"
                if any(word in content for word in ['engineer', 'technology', 'computer', 'science', 'math']):
                    tags = "STEM"
                elif any(word in content for word in ['business', 'management', 'accounting', 'finance']):
                    tags = "Business"
                elif any(word in content for word in ['art', 'design', 'music', 'creative', 'media']):
                    tags = "Arts"
                elif any(word in content for word in ['health', 'medicine', 'nursing', 'therapy']):
                    tags = "Health"
                elif any(word in content for word in ['education', 'teaching']):
                    tags = "Education"
                
                course_data = create_course_record(
                    cao_code=cao_code,
                    name=name,
                    description=f"Trinity College Dublin course",
                    nfq_level=nfq_level,
                    tags=tags,
                    course_url=course_url,
                    college_id="Trinity College Dublin"
                )
                
                courses.append(course_data)
                logger.debug(f"Extracted Trinity course {i+1}: {name}")
                
            except Exception as e:
                logger.debug(f"Error extracting course {i+1}: {e}")
                continue
        
        return courses
    
    def save_trinity_data(self, courses: List[Dict[str, Any]], filename: str = None):
        """Save Trinity course data."""
        if not courses:
            logger.warning("No Trinity courses to save")
            return
        
        if not filename:
            from datetime import datetime
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"trinity_courses_stealth_{timestamp}"
        
        # Save JSON
        json_path = RAW_DATA_DIR / f"{filename}.json"
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(courses, f, indent=2, ensure_ascii=False)
        
        # Save CSV
        csv_path = RAW_DATA_DIR / f"{filename}.csv"
        df = pd.DataFrame(courses)
        df.to_csv(csv_path, index=False, encoding='utf-8')
        
        logger.info(f"Saved {len(courses)} Trinity courses to:")
        logger.info(f"  JSON: {json_path}")
        logger.info(f"  CSV: {csv_path}")


async def main():
    """Run stealth Trinity scraping."""
    async with StealthScraper() as scraper:
        trinity_courses = await scraper.scrape_trinity_carefully()
        
        if trinity_courses:
            scraper.save_trinity_data(trinity_courses, "trinity_comprehensive_stealth")
            
            logger.info(f"Stealth Trinity scraping completed: {len(trinity_courses)} courses")
            logger.info("Sample courses:")
            for i, course in enumerate(trinity_courses[:5]):
                logger.info(f"  {i+1}. {course['name']} [{course.get('cao_code', 'No Code')}]")
        else:
            logger.warning("No Trinity courses found - IP might still be banned")


if __name__ == "__main__":
    asyncio.run(main())