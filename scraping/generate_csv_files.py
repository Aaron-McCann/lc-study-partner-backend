#!/usr/bin/env python3
"""Generate comprehensive CSV files from scraped course data."""

import asyncio
import sys
from pathlib import Path
import pandas as pd
from datetime import datetime
from loguru import logger

# Add project root to Python path
sys.path.insert(0, str(Path(__file__).parent))

from config import OUTPUT_DATA_DIR
from scrapers.qualifax_scraper import QualifaxScraper
from utils.data_cleaner import CourseDataCleaner


def setup_logging():
    """Configure logging."""
    logger.remove()
    logger.add(sys.stdout, level="INFO", format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {message}")


async def scrape_comprehensive_data():
    """Run comprehensive scraping to get more course data."""
    logger.info("Starting comprehensive course data scraping...")
    
    all_courses = []
    
    # Scrape from Qualifax
    async with QualifaxScraper() as scraper:
        logger.info("Scraping Qualifax.ie for course data...")
        courses = await scraper.run_full_scrape()
        all_courses.extend(courses)
        
        # Save raw data
        scraper.save_raw_data(f"qualifax_comprehensive_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    
    logger.info(f"Total courses scraped: {len(all_courses)}")
    return all_courses


def clean_and_organize_data(raw_courses):
    """Clean data and organize into different categories."""
    logger.info("Cleaning and organizing scraped course data...")
    
    cleaner = CourseDataCleaner()
    cleaned_courses = cleaner.clean_course_data(raw_courses)
    
    # Organize by category
    categorized_data = {
        'all_courses': cleaned_courses,
        'stem_courses': [],
        'business_courses': [],
        'arts_courses': [],
        'health_courses': [],
        'education_courses': [],
        'general_courses': []
    }
    
    # Categorize courses
    for course in cleaned_courses:
        tags = course.get('tags', '').lower()
        
        if 'stem' in tags:
            categorized_data['stem_courses'].append(course)
        elif 'business' in tags:
            categorized_data['business_courses'].append(course)
        elif 'arts' in tags:
            categorized_data['arts_courses'].append(course)
        elif 'health' in tags:
            categorized_data['health_courses'].append(course)
        elif 'education' in tags:
            categorized_data['education_courses'].append(course)
        else:
            categorized_data['general_courses'].append(course)
    
    return categorized_data


def generate_csv_files(categorized_data):
    """Generate organized CSV files from categorized data."""
    logger.info("Generating CSV files...")
    
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    generated_files = []
    
    for category, courses in categorized_data.items():
        if not courses:
            logger.info(f"No courses found for category: {category}")
            continue
        
        # Create DataFrame
        df = pd.DataFrame(courses)
        
        # Reorder columns for better readability
        column_order = [
            'name', 'cao_code', 'tags', 'nfq_level', 'duration', 
            'description', 'course_url', 'points', 'entry_requirements',
            'career_info', 'keywords', 'college_id', 'created_at'
        ]
        
        # Only include columns that exist in the data
        available_columns = [col for col in column_order if col in df.columns]
        df = df[available_columns]
        
        # Generate filename
        filename = f"{category}_{timestamp}.csv"
        filepath = OUTPUT_DATA_DIR / filename
        
        # Save CSV
        df.to_csv(filepath, index=False, encoding='utf-8')
        logger.info(f"Generated {filename}: {len(courses)} courses")
        generated_files.append(filepath)
    
    return generated_files


def create_summary_report(categorized_data, generated_files):
    """Create a summary report of the scraped data."""
    logger.info("Creating summary report...")
    
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    report_file = OUTPUT_DATA_DIR / f"scraping_summary_{timestamp}.txt"
    
    total_courses = len(categorized_data['all_courses'])
    
    report_content = f"""
CAO Course Data Scraping Summary
Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

=== SCRAPING RESULTS ===
Total Courses Scraped: {total_courses}

=== CATEGORY BREAKDOWN ===
"""
    
    for category, courses in categorized_data.items():
        if category == 'all_courses':
            continue
        count = len(courses)
        percentage = (count / total_courses * 100) if total_courses > 0 else 0
        report_content += f"{category.replace('_', ' ').title()}: {count} courses ({percentage:.1f}%)\n"
    
    report_content += f"""
=== GENERATED FILES ===
"""
    
    for filepath in generated_files:
        file_size = filepath.stat().st_size / 1024  # KB
        report_content += f"- {filepath.name} ({file_size:.1f} KB)\n"
    
    report_content += f"""
=== SAMPLE COURSES ===
"""
    
    # Add sample courses from each category
    for category, courses in categorized_data.items():
        if category == 'all_courses' or not courses:
            continue
        
        report_content += f"\n{category.replace('_', ' ').title()} Sample:\n"
        for i, course in enumerate(courses[:3]):
            name = course.get('name', 'Unknown')
            cao_code = course.get('cao_code', 'No Code')
            nfq_level = course.get('nfq_level', 'N/A')
            report_content += f"  {i+1}. {name} [{cao_code}] (Level {nfq_level})\n"
    
    report_content += f"""
=== DATA QUALITY ===
Courses with CAO Codes: {len([c for c in categorized_data['all_courses'] if c.get('cao_code')])}
Courses with Descriptions: {len([c for c in categorized_data['all_courses'] if c.get('description')])}
Courses with URLs: {len([c for c in categorized_data['all_courses'] if c.get('course_url')])}

=== USAGE INSTRUCTIONS ===
1. Import the relevant CSV file(s) into your backend database
2. Use 'all_courses_{timestamp}.csv' for complete dataset
3. Use category-specific files for filtered imports
4. Update your course service to use the new data structure
5. Consider setting up automated scraping schedule for data freshness
"""
    
    # Save report
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    logger.info(f"Summary report saved: {report_file}")
    
    # Also log key statistics
    logger.info("=== SCRAPING SUMMARY ===")
    logger.info(f"Total courses: {total_courses}")
    logger.info(f"Generated {len(generated_files)} CSV files")
    for category, courses in categorized_data.items():
        if category != 'all_courses' and courses:
            logger.info(f"  {category}: {len(courses)} courses")
    
    return report_file


async def main():
    """Main execution function."""
    setup_logging()
    
    logger.info("=== CAO Course Data CSV Generation ===")
    
    try:
        # Step 1: Scrape comprehensive data
        raw_courses = await scrape_comprehensive_data()
        
        if not raw_courses:
            logger.error("No courses were scraped. Exiting.")
            return
        
        # Step 2: Clean and organize data
        categorized_data = clean_and_organize_data(raw_courses)
        
        # Step 3: Generate CSV files
        generated_files = generate_csv_files(categorized_data)
        
        # Step 4: Create summary report
        report_file = create_summary_report(categorized_data, generated_files)
        
        logger.success("CSV generation completed successfully!")
        logger.info(f"Files generated in: {OUTPUT_DATA_DIR}")
        logger.info("Check the summary report for detailed statistics and usage instructions.")
        
    except Exception as e:
        logger.error(f"Error during CSV generation: {e}")
        raise


if __name__ == "__main__":
    asyncio.run(main())