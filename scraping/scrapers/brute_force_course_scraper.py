"""Brute force scraper to get ALL courses by iterating through course IDs."""

import asyncio
import json
import re
from typing import List, Dict, Any, Optional, Set
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


class BruteForceCourseScaper:
    """Brute force scraper that iterates through all possible course IDs."""
    
    def __init__(self, start_id: int = 1, end_id: int = 230000):
        self.base_url = "https://www.qualifax.ie"
        self.start_id = start_id
        self.end_id = end_id
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        self.all_courses: List[Dict[str, Any]] = []
        self.scraped_ids: Set[int] = set()
        self.failed_ids: Set[int] = set()
        
        # Load existing IDs if available
        self.load_existing_course_ids()
        
    def load_existing_course_ids(self):
        """Load course IDs we already have to avoid re-scraping."""
        try:
            # Check our latest dataset
            latest_csv = Path("/Users/aaronmccann/Code/LC APP/study-partner-backend/scraping/data/output/qualifax_complete_20260128_201111.csv")
            if latest_csv.exists():
                df = pd.read_csv(latest_csv)
                for url in df['course_url'].dropna():
                    if '/course/' in str(url):
                        try:
                            course_id = int(str(url).split('/course/')[-1])
                            self.scraped_ids.add(course_id)
                        except:
                            continue
                logger.info(f"Loaded {len(self.scraped_ids)} existing course IDs to skip")
        except Exception as e:
            logger.warning(f"Could not load existing IDs: {e}")
    
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
    
    async def scrape_course_by_id(self, course_id: int) -> Optional[Dict[str, Any]]:
        """Scrape a single course by its ID."""
        try:
            # Skip if already scraped
            if course_id in self.scraped_ids:
                return None
                
            course_url = f"{self.base_url}/course/{course_id}"
            
            # Navigate to course page
            await self.page.goto(course_url, timeout=30000)
            await self.page.wait_for_load_state("networkidle", timeout=10000)
            
            # Quick check if page exists
            page_content = await self.page.content()
            if '404' in page_content or 'not found' in page_content.lower() or len(page_content) < 1000:
                self.failed_ids.add(course_id)
                return None
            
            # Extract course data
            course_data = await self.extract_course_data_from_page(course_id, course_url)
            
            if course_data:
                self.scraped_ids.add(course_id)
                return course_data
            else:
                self.failed_ids.add(course_id)
                return None
                
        except Exception as e:
            logger.debug(f"Error scraping course {course_id}: {e}")
            self.failed_ids.add(course_id)
            return None
    
    async def extract_course_data_from_page(self, course_id: int, course_url: str) -> Optional[Dict[str, Any]]:
        """Extract comprehensive course data from the course page."""
        try:
            # Get course title
            name = ""
            title_selectors = ['h1', '.page-title', '.course-title', 'title']
            for selector in title_selectors:
                try:
                    title_element = await self.page.query_selector(selector)
                    if title_element:
                        title_text = await title_element.text_content()
                        if title_text and len(title_text.strip()) > 3:
                            name = clean_text(title_text)
                            break
                except:
                    continue
            
            if not name:
                return None
            
            # Get course description
            description = ""
            desc_selectors = [
                '.course-description', '.description', '.summary', 
                '.course-overview', '.overview', 'meta[name="description"]'
            ]
            for selector in desc_selectors:
                try:
                    if 'meta' in selector:
                        desc_element = await self.page.query_selector(selector)
                        if desc_element:
                            description = await desc_element.get_attribute('content')
                    else:
                        desc_element = await self.page.query_selector(selector)
                        if desc_element:
                            desc_text = await desc_element.text_content()
                            if desc_text and len(desc_text.strip()) > 20:
                                description = clean_text(desc_text)
                                break
                except:
                    continue
            
            # Extract CAO code from page content or URL
            cao_code = ""
            page_text = await self.page.content()
            cao_patterns = [
                r'CAO Code[:\s]*([A-Z]{2,4}\d{3,4})',
                r'Code[:\s]*([A-Z]{2,4}\d{3,4})',
                r'([A-Z]{2,4}\d{3,4})',  # General pattern
            ]
            
            for pattern in cao_patterns:
                match = re.search(pattern, page_text, re.IGNORECASE)
                if match:
                    cao_code = match.group(1).upper()
                    break
            
            # Extract college/institution
            college_id = ""
            institution_selectors = [
                '.institution', '.college', '.provider', 
                '.course-provider', '.institution-name'
            ]
            
            for selector in institution_selectors:
                try:
                    inst_element = await self.page.query_selector(selector)
                    if inst_element:
                        inst_text = await inst_element.text_content()
                        if inst_text and len(inst_text.strip()) > 2:
                            college_id = clean_text(inst_text)
                            break
                except:
                    continue
            
            # If no institution found, try to extract from page content
            if not college_id:
                for uni in ['Trinity College Dublin', 'University College Dublin', 'University College Cork', 'Dublin City University', 'Maynooth University']:
                    if uni.lower() in page_text.lower():
                        college_id = uni
                        break
            
            # Extract NFQ Level
            nfq_level = 8  # Default
            nfq_patterns = [
                r'NFQ Level (\d+)',
                r'Level (\d+)',
                r'QQI Level (\d+)'
            ]
            
            for pattern in nfq_patterns:
                match = re.search(pattern, page_text, re.IGNORECASE)
                if match:
                    nfq_level = int(match.group(1))
                    break
            
            # If no explicit level found, infer from course name/description
            content = f"{name} {description}".lower()
            if any(word in content for word in ['certificate', 'cert']):
                nfq_level = 6
            elif 'diploma' in content:
                nfq_level = 7
            elif any(word in content for word in ['master', 'msc', 'ma ', 'mba', 'med']):
                nfq_level = 9
            elif any(word in content for word in ['phd', 'doctorate', 'doctoral']):
                nfq_level = 10
            
            # Extract course points (CAO points)
            points = 0
            points_patterns = [
                r'CAO Points[:\s]*(\d+)',
                r'Points[:\s]*(\d+)',
                r'(\d{3,4}) points'
            ]
            
            for pattern in points_patterns:
                match = re.search(pattern, page_text, re.IGNORECASE)
                if match:
                    points = int(match.group(1))
                    break
            
            # Determine category from course content
            tags = "General"
            if any(word in content for word in ['engineer', 'technology', 'computer', 'science', 'data', 'software', 'math']):
                tags = "STEM"
            elif any(word in content for word in ['business', 'management', 'accounting', 'finance', 'marketing', 'commerce']):
                tags = "Business"
            elif any(word in content for word in ['art', 'design', 'music', 'creative', 'media', 'film', 'drama']):
                tags = "Arts"
            elif any(word in content for word in ['health', 'medicine', 'nursing', 'therapy', 'medical', 'care', 'pharmacy']):
                tags = "Health"
            elif any(word in content for word in ['education', 'teaching', 'early learning', 'childcare', 'pedagogy']):
                tags = "Education"
            
            return create_course_record(
                cao_code=cao_code,
                name=name,
                description=description,
                nfq_level=nfq_level,
                tags=tags,
                course_url=course_url,
                points=points,
                college_id=college_id or "Unknown"
            )
            
        except Exception as e:
            logger.debug(f"Error extracting data for course {course_id}: {e}")
            return None
    
    async def run_brute_force_scraping(self, batch_size: int = 10, max_courses: Optional[int] = None) -> List[Dict[str, Any]]:
        """Run brute force scraping through all course IDs."""
        logger.info(f"Starting brute force scraping from ID {self.start_id} to {self.end_id}")
        
        # Calculate range excluding already scraped
        total_range = self.end_id - self.start_id + 1
        already_scraped = len([id for id in self.scraped_ids if self.start_id <= id <= self.end_id])
        remaining_ids = total_range - already_scraped
        
        logger.info(f"Total range: {total_range}, Already scraped: {already_scraped}, Remaining: {remaining_ids}")
        
        if max_courses:
            remaining_ids = min(remaining_ids, max_courses)
            logger.info(f"Limited to {max_courses} courses for this run")
        
        # Progress tracking
        courses_found = 0
        courses_processed = 0
        
        # Process in batches to manage memory and provide checkpoints
        current_id = self.start_id
        
        pbar = tqdm(total=remaining_ids, desc="Brute force scraping")
        
        while current_id <= self.end_id and (not max_courses or courses_found < max_courses):
            batch_courses = []
            batch_start = current_id
            
            # Process batch
            for _ in range(batch_size):
                if current_id > self.end_id:
                    break
                if max_courses and courses_found >= max_courses:
                    break
                
                # Skip if already processed
                if current_id not in self.scraped_ids and current_id not in self.failed_ids:
                    try:
                        course_data = await self.scrape_course_by_id(current_id)
                        if course_data:
                            batch_courses.append(course_data)
                            courses_found += 1
                    except Exception as e:
                        logger.debug(f"Error processing course {current_id}: {e}")
                    
                    courses_processed += 1
                    pbar.update(1)
                    pbar.set_postfix({
                        'found': courses_found, 
                        'processed': courses_processed,
                        'current_id': current_id
                    })
                
                current_id += 1
                
                # Small delay to be respectful
                await asyncio.sleep(0.1)
            
            # Add batch to main collection
            self.all_courses.extend(batch_courses)
            
            # Save progress every 100 courses or 1000 processed IDs
            if len(self.all_courses) % 100 == 0 or courses_processed % 1000 == 0:
                await self.save_progress()
            
            logger.info(f"Batch {batch_start}-{current_id-1}: Found {len(batch_courses)} courses. Total: {courses_found}")
            
            # Longer delay between batches
            await asyncio.sleep(1)
        
        pbar.close()
        
        logger.success(f"Brute force scraping completed! Found {courses_found} new courses")
        return self.all_courses
    
    async def save_progress(self):
        """Save current progress to avoid losing data."""
        if not self.all_courses:
            return
        
        timestamp = int(time.time())
        progress_file = RAW_DATA_DIR / f"brute_force_progress_{timestamp}.json"
        
        progress_data = {
            'courses': self.all_courses,
            'scraped_ids': list(self.scraped_ids),
            'failed_ids': list(self.failed_ids),
            'timestamp': timestamp
        }
        
        with open(progress_file, 'w', encoding='utf-8') as f:
            json.dump(progress_data, f, indent=2, ensure_ascii=False)
        
        logger.debug(f"Progress saved: {len(self.all_courses)} courses")
    
    def save_data(self, filename: str = None):
        """Save final scraped data."""
        if not self.all_courses:
            logger.warning("No courses to save")
            return
        
        if not filename:
            from datetime import datetime
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"qualifax_brute_force_{timestamp}"
        
        # Save JSON
        json_path = RAW_DATA_DIR / f"{filename}.json"
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(self.all_courses, f, indent=2, ensure_ascii=False)
        
        # Save CSV
        csv_path = RAW_DATA_DIR / f"{filename}.csv"
        df = pd.DataFrame(self.all_courses)
        df.to_csv(csv_path, index=False, encoding='utf-8')
        
        # Save metadata
        meta_path = RAW_DATA_DIR / f"{filename}_metadata.json"
        metadata = {
            'total_courses': len(self.all_courses),
            'scraped_ids_count': len(self.scraped_ids),
            'failed_ids_count': len(self.failed_ids),
            'id_range': f"{self.start_id}-{self.end_id}",
            'scraped_ids': list(self.scraped_ids),
            'failed_ids': list(self.failed_ids)
        }
        
        with open(meta_path, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Saved {len(self.all_courses)} courses to:")
        logger.info(f"  JSON: {json_path}")
        logger.info(f"  CSV: {csv_path}")
        logger.info(f"  Metadata: {meta_path}")


async def main():
    """Test the brute force scraper."""
    # Start with a small range for testing
    async with BruteForceCourseScaper(start_id=1, end_id=1000) as scraper:
        # Test with first 100 courses
        courses = await scraper.run_brute_force_scraping(batch_size=5, max_courses=100)
        scraper.save_data("qualifax_brute_force_test")
        
        logger.info(f"Brute force test completed: {len(courses)} courses found")
        
        if courses:
            logger.info("Sample brute force courses:")
            for i, course in enumerate(courses[:5]):
                logger.info(f"{i+1}. {course['name']} [{course.get('cao_code', 'No Code')}] - {course.get('college_id', 'Unknown')}")


if __name__ == "__main__":
    asyncio.run(main())