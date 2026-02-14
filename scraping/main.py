#!/usr/bin/env python3
"""Main script for CAO course data scraping."""

import asyncio
import sys
from pathlib import Path
from loguru import logger

# Add project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from config import LOGGING_CONFIG, SOURCES
from scrapers.qualifax_scraper import QualifaxScraper


def setup_logging():
    """Configure logging for the scraping project."""
    logger.remove()  # Remove default handler
    logger.add(
        sys.stdout,
        level=LOGGING_CONFIG["level"],
        format=LOGGING_CONFIG["format"]
    )
    
    # Also log to file
    logger.add(
        project_root / "scraping.log",
        level=LOGGING_CONFIG["level"],
        format=LOGGING_CONFIG["format"],
        rotation=LOGGING_CONFIG["rotation"],
        retention=LOGGING_CONFIG["retention"]
    )


async def run_qualifax_scraper():
    """Run the Qualifax scraper."""
    logger.info("Starting Qualifax course data scraping...")
    
    try:
        async with QualifaxScraper() as scraper:
            courses = await scraper.run_full_scrape()
            scraper.save_raw_data()
            
            if courses:
                logger.success(f"Successfully scraped {len(courses)} courses from Qualifax")
                
                # Show sample of results
                logger.info("Sample courses:")
                for i, course in enumerate(courses[:3]):
                    logger.info(f"{i+1}. {course.get('name', 'N/A')} [{course.get('cao_code', 'No Code')}]")
                    if course.get('description'):
                        logger.info(f"   Description: {course['description'][:100]}...")
                
                return courses
            else:
                logger.warning("No courses were scraped from Qualifax")
                return []
                
    except Exception as e:
        logger.error(f"Error running Qualifax scraper: {e}")
        return []


async def main():
    """Main execution function."""
    setup_logging()
    
    logger.info("=== CAO Course Data Scraping Tool ===")
    logger.info(f"Sources enabled: {[name for name, config in SOURCES.items() if config.get('enabled')]}")
    
    all_courses = []
    
    # Run Qualifax scraper if enabled
    if SOURCES["qualifax"]["enabled"]:
        qualifax_courses = await run_qualifax_scraper()
        all_courses.extend(qualifax_courses)
    
    # TODO: Add CAO scraper when ready
    # if SOURCES["cao"]["enabled"]:
    #     cao_courses = await run_cao_scraper()
    #     all_courses.extend(cao_courses)
    
    logger.info(f"Total courses scraped: {len(all_courses)}")
    
    if all_courses:
        logger.success("Scraping completed successfully!")
    else:
        logger.warning("No courses were scraped from any source.")
    
    return all_courses


if __name__ == "__main__":
    try:
        courses = asyncio.run(main())
        sys.exit(0 if courses else 1)
    except KeyboardInterrupt:
        logger.info("Scraping interrupted by user")
        sys.exit(130)
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)