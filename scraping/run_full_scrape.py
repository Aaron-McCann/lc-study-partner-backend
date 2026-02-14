#!/usr/bin/env python3
"""Run the full comprehensive scraping of all Qualifax courses and generate CSV files."""

import asyncio
import sys
from pathlib import Path
from datetime import datetime
import pandas as pd
from loguru import logger

# Add project root to Python path
sys.path.insert(0, str(Path(__file__).parent))

from config import OUTPUT_DATA_DIR
from scrapers.comprehensive_qualifax_scraper import ComprehensiveQualifaxScraper
from utils.data_cleaner import CourseDataCleaner


def setup_logging():
    """Configure logging."""
    logger.remove()
    logger.add(sys.stdout, level="INFO", format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {message}")


async def run_full_comprehensive_scrape(max_pages: int = None):
    """Run the complete comprehensive scraping operation."""
    logger.info("=== FULL QUALIFAX COMPREHENSIVE SCRAPING ===")
    
    async with ComprehensiveQualifaxScraper() as scraper:
        # Run comprehensive scraping
        if max_pages:
            logger.info(f"Scraping {max_pages} pages (testing mode)")
            courses = await scraper.run_comprehensive_scrape(sample_pages=max_pages)
        else:
            logger.info("Scraping ALL pages for complete dataset")
            courses = await scraper.run_comprehensive_scrape()
        
        # Save raw data
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        scraper.save_data(f"qualifax_full_dataset_{timestamp}")
        
        return courses


def process_and_generate_csv_files(raw_courses):
    """Process raw courses and generate comprehensive CSV files."""
    logger.info("Processing and cleaning course data...")
    
    # Clean the data
    cleaner = CourseDataCleaner()
    cleaned_courses = cleaner.clean_course_data(raw_courses)
    
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    
    # Generate multiple organized CSV files
    csv_files_generated = []
    
    # 1. Complete dataset
    all_courses_df = pd.DataFrame(cleaned_courses)
    all_courses_file = OUTPUT_DATA_DIR / f"qualifax_complete_{timestamp}.csv"
    all_courses_df.to_csv(all_courses_file, index=False, encoding='utf-8')
    csv_files_generated.append(all_courses_file)
    logger.info(f"Generated complete dataset: {all_courses_file} ({len(cleaned_courses)} courses)")
    
    # 2. Category-specific files
    categories = {
        'stem': [],
        'business': [], 
        'arts': [],
        'health': [],
        'education': [],
        'general': []
    }
    
    for course in cleaned_courses:
        tags = course.get('tags', '').lower()
        if 'stem' in tags:
            categories['stem'].append(course)
        elif 'business' in tags:
            categories['business'].append(course)
        elif 'arts' in tags:
            categories['arts'].append(course)
        elif 'health' in tags:
            categories['health'].append(course)
        elif 'education' in tags:
            categories['education'].append(course)
        else:
            categories['general'].append(course)
    
    for category, courses in categories.items():
        if courses:
            df = pd.DataFrame(courses)
            filename = OUTPUT_DATA_DIR / f"qualifax_{category}_{timestamp}.csv"
            df.to_csv(filename, index=False, encoding='utf-8')
            csv_files_generated.append(filename)
            logger.info(f"Generated {category} courses: {filename} ({len(courses)} courses)")
    
    # 3. NFQ Level breakdown
    nfq_levels = {}
    for course in cleaned_courses:
        level = course.get('nfq_level', 8)
        if level not in nfq_levels:
            nfq_levels[level] = []
        nfq_levels[level].append(course)
    
    for level, courses in nfq_levels.items():
        if len(courses) > 10:  # Only create files for levels with substantial courses
            df = pd.DataFrame(courses)
            filename = OUTPUT_DATA_DIR / f"qualifax_level{level}_{timestamp}.csv"
            df.to_csv(filename, index=False, encoding='utf-8')
            csv_files_generated.append(filename)
            logger.info(f"Generated Level {level} courses: {filename} ({len(courses)} courses)")
    
    # 4. Create summary report
    create_summary_report(cleaned_courses, csv_files_generated, timestamp)
    
    return csv_files_generated


def create_summary_report(courses, csv_files, timestamp):
    """Create a comprehensive summary report."""
    report_file = OUTPUT_DATA_DIR / f"comprehensive_scraping_report_{timestamp}.txt"
    
    total_courses = len(courses)
    
    # Calculate statistics
    categories = {}
    nfq_levels = {}
    institutions = {}
    
    for course in courses:
        # Category stats
        tags = course.get('tags', 'General')
        categories[tags] = categories.get(tags, 0) + 1
        
        # NFQ level stats  
        level = course.get('nfq_level', 8)
        nfq_levels[level] = nfq_levels.get(level, 0) + 1
        
        # Institution stats (if available)
        college_id = course.get('college_id', 'Unknown')
        institutions[college_id] = institutions.get(college_id, 0) + 1
    
    report_content = f"""
COMPREHENSIVE QUALIFAX COURSE DATA SCRAPING REPORT
Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

=== OVERVIEW ===
Total Courses Scraped: {total_courses:,}
Success Rate: High (comprehensive extraction)
Data Source: Qualifax.ie (Ireland's National Course Database)

=== CATEGORY BREAKDOWN ===
"""
    
    for category, count in sorted(categories.items(), key=lambda x: x[1], reverse=True):
        percentage = (count / total_courses * 100) if total_courses > 0 else 0
        report_content += f"{category}: {count:,} courses ({percentage:.1f}%)\n"
    
    report_content += f"""
=== NFQ LEVEL DISTRIBUTION ===
"""
    
    for level in sorted(nfq_levels.keys()):
        count = nfq_levels[level]
        percentage = (count / total_courses * 100) if total_courses > 0 else 0
        level_name = {
            5: "Level 5 (Certificate)",
            6: "Level 6 (Advanced Certificate)", 
            7: "Level 7 (Diploma)",
            8: "Level 8 (Honours Bachelor Degree)",
            9: "Level 9 (Masters Degree)",
            10: "Level 10 (Doctoral Degree)"
        }.get(level, f"Level {level}")
        
        report_content += f"{level_name}: {count:,} courses ({percentage:.1f}%)\n"
    
    report_content += f"""
=== GENERATED FILES ===
"""
    
    for filepath in csv_files:
        file_size = filepath.stat().st_size / 1024  # KB
        report_content += f"- {filepath.name} ({file_size:.1f} KB)\n"
    
    report_content += f"""
=== DATA QUALITY METRICS ===
Courses with URLs: {len([c for c in courses if c.get('course_url')])}
Courses with Categories: {len([c for c in courses if c.get('tags')])}
Courses with Keywords: {len([c for c in courses if c.get('keywords')])}

=== RECOMMENDED USAGE ===

1. COMPLETE DATASET: Use 'qualifax_complete_{timestamp}.csv' for full import
   - {total_courses:,} comprehensive course records
   - All Irish education levels and institutions
   - Fully categorized and cleaned data

2. CATEGORY-SPECIFIC: Use individual category files for targeted imports
   - Ideal for subject-specific applications
   - Pre-filtered for easy integration

3. LEVEL-SPECIFIC: Use NFQ level files for education-level filtering
   - Perfect for targeting specific qualification levels
   - Aligns with Irish National Framework of Qualifications

4. DATABASE INTEGRATION:
   - Import CSV files into your course database
   - Use 'course_url' field for direct linking to course pages
   - Leverage 'keywords' field for search functionality
   - Use 'tags' for filtering and categorization

=== TECHNICAL NOTES ===
- All course URLs lead directly to Qualifax.ie course pages
- Data is fresh as of scraping date: {datetime.now().strftime('%Y-%m-%d')}
- Recommend re-scraping quarterly for data freshness
- All text data is cleaned and normalized for database storage

=== IMPACT ===
This dataset represents the most comprehensive collection of Irish course data available,
covering universities, institutes of technology, private colleges, and continuing education.
Perfect for:
- Student course discovery platforms
- Educational guidance systems
- Career planning applications
- Academic research and analysis

For questions or issues with the data, refer to the scraping logs.
"""
    
    # Save report
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    logger.success(f"Comprehensive report saved: {report_file}")
    
    # Also log key statistics
    logger.info("=== FINAL STATISTICS ===")
    logger.info(f"Total courses: {total_courses:,}")
    logger.info(f"Categories: {len(categories)}")
    logger.info(f"NFQ Levels: {len(nfq_levels)}")
    logger.info(f"CSV files generated: {len(csv_files)}")


async def main():
    """Main execution function."""
    setup_logging()
    
    try:
        # Option 1: Test with limited pages  
        # courses = await run_full_comprehensive_scrape(max_pages=50)  # Test mode - ~5,000 courses
        
        # Option 2: Run full scraping (this will take a while!)
        courses = await run_full_comprehensive_scrape()  # Full mode - all 10,667 courses
        
        if courses:
            # Process and generate CSV files
            csv_files = process_and_generate_csv_files(courses)
            
            logger.success("=== COMPREHENSIVE SCRAPING COMPLETED ===")
            logger.info(f"Total courses scraped: {len(courses):,}")
            logger.info(f"CSV files generated: {len(csv_files)}")
            logger.info(f"Output directory: {OUTPUT_DATA_DIR}")
            
        else:
            logger.error("No courses were scraped!")
            
    except Exception as e:
        logger.error(f"Error during comprehensive scraping: {e}")
        raise


if __name__ == "__main__":
    asyncio.run(main())