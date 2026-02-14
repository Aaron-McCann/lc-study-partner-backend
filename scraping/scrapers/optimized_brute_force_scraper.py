"""Optimized brute force scraper for massive course ID ranges."""

import asyncio
import json
import re
from typing import List, Dict, Any, Optional, Set
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


class OptimizedBruteForceScraper:
    """Highly optimized scraper for large course ID ranges."""
    
    def __init__(self, start_id: int = 1, end_id: int = 230000):
        self.base_url = "https://www.qualifax.ie"
        self.start_id = start_id
        self.end_id = end_id
        self.browser: Optional[Browser] = None
        self.page: Optional[Page] = None
        self.all_courses: List[Dict[str, Any]] = []
        self.scraped_ids: Set[int] = set()
        self.failed_ids: Set[int] = set()
        
        # Priority ranges where university courses are likely to be
        self.priority_ranges = [
            (1, 10000),          # Early courses
            (50000, 70000),      # Mid-range
            (100000, 120000),    # High range
            (200000, 230000),    # Latest courses
        ]
        
        # Load existing IDs
        self.load_existing_course_ids()
        
    def load_existing_course_ids(self):
        """Load course IDs we already have."""
        try:
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
        
        # Optimize for speed
        await self.page.set_extra_http_headers({
            'User-Agent': get_random_user_agent()
        })
        
        # Disable images and CSS for faster loading
        await self.page.route("**/*.{png,jpg,jpeg,gif,css,woff,woff2}", lambda route: route.abort())
        
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        if self.browser:
            await self.browser.close()
    
    async def quick_check_exists(self, course_id: int) -> bool:
        """Quick check if course exists without full scraping."""
        try:
            course_url = f"{self.base_url}/course/{course_id}"
            response = await self.page.goto(course_url, timeout=10000)
            
            # Quick checks for non-existence
            if response.status == 404:
                return False
                
            # Check page title quickly
            title = await self.page.title()
            if '404' in title.lower() or 'not found' in title.lower():
                return False
                
            return True
            
        except:
            return False
    
    async def scrape_course_by_id(self, course_id: int) -> Optional[Dict[str, Any]]:
        """Scrape a single course by its ID with optimizations."""
        try:
            if course_id in self.scraped_ids or course_id in self.failed_ids:
                return None
                
            # Quick existence check first
            if not await self.quick_check_exists(course_id):
                self.failed_ids.add(course_id)
                return None
            
            course_url = f"{self.base_url}/course/{course_id}"
            
            # Extract minimal required data for speed
            course_data = await self.extract_minimal_course_data(course_id, course_url)
            
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
    
    async def extract_minimal_course_data(self, course_id: int, course_url: str) -> Optional[Dict[str, Any]]:
        """Extract minimal course data for speed."""
        try:
            # Get course title - most important
            name = await self.page.title()
            if not name or len(name.strip()) < 3:
                return None
            
            name = clean_text(name)
            
            # Quick extraction of key data
            page_text = await self.page.content()
            
            # Extract CAO code quickly
            cao_code = ""
            cao_match = re.search(r'([A-Z]{2,4}\d{3,4})', page_text[:2000])  # Check first 2000 chars only
            if cao_match:
                cao_code = cao_match.group(1)
            
            # Quick institution detection
            college_id = "Unknown"
            institutions = [
                'Trinity College Dublin', 'University College Dublin', 
                'University College Cork', 'Dublin City University',
                'Maynooth University', 'University of Limerick',
                'University of Galway'
            ]
            
            for inst in institutions:
                if inst.lower() in page_text.lower():
                    college_id = inst
                    break
            
            # Quick NFQ level detection
            nfq_level = 8  # Default
            nfq_match = re.search(r'NFQ Level (\d+)', page_text[:1000])
            if nfq_match:
                nfq_level = int(nfq_match.group(1))
            
            # Quick category detection
            tags = "General"
            content_lower = name.lower()
            if any(word in content_lower for word in ['engineer', 'technology', 'computer', 'science']):
                tags = "STEM"
            elif any(word in content_lower for word in ['business', 'management', 'accounting']):
                tags = "Business"
            elif any(word in content_lower for word in ['art', 'design', 'music']):
                tags = "Arts"
            elif any(word in content_lower for word in ['health', 'medicine', 'nursing']):
                tags = "Health"
            elif any(word in content_lower for word in ['education', 'teaching']):
                tags = "Education"
            
            return create_course_record(
                cao_code=cao_code,
                name=name,
                description=f"Course from brute force scraping (ID: {course_id})",
                nfq_level=nfq_level,
                tags=tags,
                course_url=course_url,
                college_id=college_id
            )
            
        except Exception as e:
            logger.debug(f"Error extracting data for course {course_id}: {e}")
            return None
    
    async def run_priority_scraping(self, target_courses: int = 1000) -> List[Dict[str, Any]]:
        """Run optimized scraping focusing on priority ranges."""
        logger.info(f"Starting priority range scraping for {target_courses} courses")
        
        courses_found = 0
        total_processed = 0
        
        # Process priority ranges first
        for start_range, end_range in self.priority_ranges:
            if courses_found >= target_courses:
                break
                
            logger.info(f"Processing priority range {start_range}-{end_range}")
            
            # Sample every 5th ID in priority ranges for speed
            sample_ids = list(range(start_range, end_range, 5))
            remaining_ids = [id for id in sample_ids if id not in self.scraped_ids and id not in self.failed_ids]
            
            logger.info(f"Checking {len(remaining_ids)} IDs in range {start_range}-{end_range}")
            
            with tqdm(total=min(len(remaining_ids), target_courses - courses_found), 
                     desc=f"Range {start_range}-{end_range}") as pbar:
                
                for course_id in remaining_ids:
                    if courses_found >= target_courses:
                        break
                    
                    try:
                        course_data = await self.scrape_course_by_id(course_id)
                        if course_data:
                            self.all_courses.append(course_data)
                            courses_found += 1
                            
                            # Log significant finds
                            if course_data.get('college_id') != 'Unknown':
                                logger.info(f"Found course from {course_data['college_id']}: {course_data['name']}")
                    
                    except Exception as e:
                        logger.debug(f"Error processing {course_id}: {e}")
                    
                    total_processed += 1
                    pbar.update(1)
                    pbar.set_postfix({'found': courses_found, 'total': total_processed})
                    
                    # Minimal delay for speed
                    await asyncio.sleep(0.05)
                    
                    # Save progress periodically
                    if courses_found > 0 and courses_found % 50 == 0:
                        await self.save_progress()
                
            logger.info(f"Range {start_range}-{end_range} completed. Total found: {courses_found}")
            
            # Short break between ranges
            await asyncio.sleep(1)
        
        logger.success(f"Priority scraping completed! Found {courses_found} courses from {total_processed} IDs")
        return self.all_courses
    
    async def run_targeted_university_search(self, target_courses: int = 500) -> List[Dict[str, Any]]:
        """Target specific ID ranges where university courses are likely."""
        logger.info("Running targeted search for university courses")
        
        # These ranges are more likely to contain university courses based on patterns
        university_likely_ranges = [
            (2000, 5000),    # Early university registrations
            (25000, 35000),  # Mid-period university courses  
            (50000, 60000),  # Major university update period
            (90000, 110000), # Recent university additions
            (200000, 220000) # Latest university courses
        ]
        
        courses_found = 0
        
        for start_range, end_range in university_likely_ranges:
            if courses_found >= target_courses:
                break
                
            logger.info(f"Searching university courses in range {start_range}-{end_range}")
            
            # Check every 10th ID for speed while still covering range
            sample_ids = list(range(start_range, end_range, 10))
            remaining_ids = [id for id in sample_ids if id not in self.scraped_ids]
            
            for course_id in remaining_ids[:100]:  # Limit per range for speed
                try:
                    course_data = await self.scrape_course_by_id(course_id)
                    if course_data and course_data.get('college_id') != 'Unknown':
                        self.all_courses.append(course_data)
                        courses_found += 1
                        logger.info(f"Found university course: {course_data['name']} - {course_data['college_id']}")
                        
                        if courses_found >= target_courses:
                            break
                
                except Exception as e:
                    logger.debug(f"Error processing {course_id}: {e}")
                
                await asyncio.sleep(0.02)
        
        return self.all_courses
    
    async def save_progress(self):
        """Save current progress."""
        if not self.all_courses:
            return
        
        timestamp = int(time.time())
        progress_file = RAW_DATA_DIR / f"optimized_brute_force_progress_{timestamp}.json"
        
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
        """Save final data."""
        if not self.all_courses:
            logger.warning("No courses to save")
            return
        
        if not filename:
            from datetime import datetime
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"qualifax_optimized_brute_force_{timestamp}"
        
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
    """Run optimized brute force scraping."""
    async with OptimizedBruteForceScraper() as scraper:
        # First try targeted university search
        await scraper.run_targeted_university_search(target_courses=200)
        
        # Then broader priority range search
        await scraper.run_priority_scraping(target_courses=300)
        
        scraper.save_data("qualifax_optimized_missing_courses")
        
        logger.info(f"Optimized scraping completed: {len(scraper.all_courses)} new courses found")
        
        # Count university courses found
        university_courses = [c for c in scraper.all_courses if c.get('college_id') != 'Unknown']
        logger.info(f"University courses found: {len(university_courses)}")
        
        for course in university_courses:
            logger.info(f"  - {course['name']} [{course['college_id']}]")


if __name__ == "__main__":
    asyncio.run(main())