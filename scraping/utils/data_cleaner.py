"""Data cleaning and validation utilities for scraped course data."""

import pandas as pd
import re
from typing import List, Dict, Any, Optional, Set
from loguru import logger
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).parent.parent))
from config import VALIDATION_RULES, PROCESSED_DATA_DIR


class CourseDataCleaner:
    """Clean and validate scraped course data."""
    
    def __init__(self):
        self.validation_stats = {
            "total_records": 0,
            "valid_records": 0,
            "duplicates_removed": 0,
            "validation_errors": {}
        }
    
    def clean_course_data(self, courses: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Clean and validate a list of course records."""
        logger.info(f"Starting data cleaning for {len(courses)} course records")
        
        self.validation_stats["total_records"] = len(courses)
        
        # Step 1: Basic cleaning
        cleaned_courses = [self.clean_single_course(course) for course in courses]
        
        # Step 2: Remove invalid records
        valid_courses = [course for course in cleaned_courses if self.validate_course(course)]
        
        # Step 3: Remove duplicates
        unique_courses = self.remove_duplicates(valid_courses)
        
        # Step 4: Enhance data
        enhanced_courses = [self.enhance_course_data(course) for course in unique_courses]
        
        self.validation_stats["valid_records"] = len(enhanced_courses)
        
        logger.info(f"Data cleaning completed: {len(enhanced_courses)} valid records from {len(courses)} original")
        self.log_validation_stats()
        
        return enhanced_courses
    
    def clean_single_course(self, course: Dict[str, Any]) -> Dict[str, Any]:
        """Clean a single course record."""
        cleaned = course.copy()
        
        # Clean text fields
        text_fields = ['name', 'description', 'duration', 'tags', 'entry_requirements', 'career_info']
        for field in text_fields:
            if field in cleaned:
                cleaned[field] = self.clean_text_field(cleaned[field])
        
        # Clean and standardize cao_code
        if 'cao_code' in cleaned:
            cleaned['cao_code'] = self.standardize_cao_code(cleaned['cao_code'])
        
        # Ensure numeric fields are proper types
        if 'nfq_level' in cleaned:
            cleaned['nfq_level'] = self.clean_nfq_level(cleaned['nfq_level'])
        
        if 'points' in cleaned:
            cleaned['points'] = self.clean_points(cleaned['points'])
        
        if 'college_id' in cleaned:
            cleaned['college_id'] = self.clean_college_id(cleaned['college_id'])
        
        return cleaned
    
    def clean_text_field(self, text: Any) -> str:
        """Clean text field with comprehensive normalization."""
        if not text:
            return ""
        
        text = str(text).strip()
        
        # Remove excessive whitespace
        text = re.sub(r'\s+', ' ', text)
        
        # Fix encoding issues
        text = text.replace('\u00a0', ' ')  # Non-breaking space
        text = text.replace('\u2019', "'")  # Right single quotation mark
        text = text.replace('\u201c', '"')  # Left double quotation mark
        text = text.replace('\u201d', '"')  # Right double quotation mark
        text = text.replace('\u2013', '-')  # En dash
        text = text.replace('\u2014', '-')  # Em dash
        
        # Remove unwanted characters
        text = re.sub(r'[^\w\s\-.,()&/]', '', text)
        
        return text.strip()
    
    def standardize_cao_code(self, code: Any) -> str:
        """Standardize CAO codes to consistent format."""
        if not code:
            return ""
        
        code = str(code).strip().upper()
        
        # Extract valid CAO code pattern
        match = re.search(r'([A-Z]{2,4})(\d{3,4})', code)
        if match:
            return f"{match.group(1)}{match.group(2)}"
        
        # If no standard pattern found, return cleaned version
        return re.sub(r'[^A-Z0-9]', '', code) if code else ""
    
    def clean_nfq_level(self, level: Any) -> int:
        """Clean and validate NFQ level."""
        if not level:
            return 8  # Default to Level 8 (Honours Bachelor)
        
        try:
            level = int(level)
            if level in VALIDATION_RULES["valid_nfq_levels"]:
                return level
            else:
                logger.debug(f"Invalid NFQ level {level}, defaulting to 8")
                return 8
        except (ValueError, TypeError):
            logger.debug(f"Could not parse NFQ level '{level}', defaulting to 8")
            return 8
    
    def clean_points(self, points: Any) -> int:
        """Clean and validate CAO points."""
        if not points:
            return 0
        
        try:
            points = int(points)
            # CAO points typically range from 0 to 625
            if 0 <= points <= 625:
                return points
            else:
                logger.debug(f"Invalid points value {points}, setting to 0")
                return 0
        except (ValueError, TypeError):
            logger.debug(f"Could not parse points '{points}', setting to 0")
            return 0
    
    def clean_college_id(self, college_id: Any) -> int:
        """Clean and validate college ID."""
        if not college_id:
            return 1  # Default college ID
        
        try:
            college_id = int(college_id)
            if college_id > 0:
                return college_id
            else:
                return 1
        except (ValueError, TypeError):
            return 1
    
    def validate_course(self, course: Dict[str, Any]) -> bool:
        """Validate a single course record against rules."""
        errors = []
        
        # Check required fields
        for field in VALIDATION_RULES["required_fields"]:
            if field not in course or not course[field]:
                errors.append(f"Missing required field: {field}")
        
        # Check minimum description length (only if description exists)
        description = course.get('description', '')
        if description and len(description) < VALIDATION_RULES["min_description_length"]:
            # Just warn, don't fail validation for missing description
            logger.debug(f"Short description for {course.get('name', 'Unknown')}: {len(description)} chars")
        
        # Check course name length
        name = course.get('name', '')
        if name and len(name) > VALIDATION_RULES["max_course_name_length"]:
            errors.append(f"Course name too long: {len(name)} chars")
        
        # Check minimum course name length and filter out non-course entries
        if not name or len(name) < VALIDATION_RULES.get("min_course_name_length", 3):
            errors.append(f"Course name too short: {len(name) if name else 0} chars")
        
        # Filter out clearly non-course entries
        if name:
            invalid_names = ['about us', 'contact us', 'help', 'privacy', 'cookie', 'accessibility', 'terms']
            if any(invalid in name.lower() for invalid in invalid_names):
                errors.append("Not a course entry (system page)")
        
        # Check NFQ level
        nfq_level = course.get('nfq_level')
        if nfq_level not in VALIDATION_RULES["valid_nfq_levels"]:
            errors.append(f"Invalid NFQ level: {nfq_level}")
        
        if errors:
            course_name = course.get('name', 'Unknown')[:50]
            self.validation_stats["validation_errors"][course_name] = errors
            logger.debug(f"Validation failed for '{course_name}': {errors}")
            return False
        
        return True
    
    def remove_duplicates(self, courses: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Remove duplicate courses based on name and code."""
        seen_keys: Set[str] = set()
        unique_courses = []
        duplicates_count = 0
        
        for course in courses:
            # Create a key for deduplication (name + cao_code)
            name = course.get('name', '').lower().strip()
            cao_code = course.get('cao_code', '').strip()
            
            # If no CAO code, use just the name
            key = f"{name}|{cao_code}" if cao_code else name
            
            if key and key not in seen_keys:
                seen_keys.add(key)
                unique_courses.append(course)
            else:
                duplicates_count += 1
                logger.debug(f"Removed duplicate: {name}")
        
        self.validation_stats["duplicates_removed"] = duplicates_count
        logger.info(f"Removed {duplicates_count} duplicate records")
        
        return unique_courses
    
    def enhance_course_data(self, course: Dict[str, Any]) -> Dict[str, Any]:
        """Enhance course data with derived fields and improvements."""
        enhanced = course.copy()
        
        # Auto-detect missing CAO codes from name patterns
        if not enhanced.get('cao_code'):
            enhanced['cao_code'] = self.extract_cao_code_from_name(enhanced.get('name', ''))
        
        # Improve tags based on course content
        enhanced['tags'] = self.improve_tags(enhanced)
        
        # Standardize duration format
        enhanced['duration'] = self.standardize_duration(enhanced.get('duration', ''))
        
        # Add searchable keywords
        enhanced['keywords'] = self.generate_keywords(enhanced)
        
        return enhanced
    
    def extract_cao_code_from_name(self, name: str) -> str:
        """Try to extract CAO code from course name."""
        if not name:
            return ""
        
        # Look for patterns like "AB123" or "ABC1234" in the name
        match = re.search(r'\b([A-Z]{2,4}\d{3,4})\b', name.upper())
        if match:
            return match.group(1)
        
        return ""
    
    def improve_tags(self, course: Dict[str, Any]) -> str:
        """Improve course tags based on course content."""
        name = course.get('name', '').lower()
        description = course.get('description', '').lower()
        content = f"{name} {description}"
        
        tags = set()
        
        # STEM subjects
        stem_keywords = ['engineer', 'computer', 'science', 'technology', 'math', 'physics', 'chemistry', 'biology', 'data']
        if any(keyword in content for keyword in stem_keywords):
            tags.add('STEM')
        
        # Business subjects
        business_keywords = ['business', 'management', 'accounting', 'finance', 'marketing', 'economics']
        if any(keyword in content for keyword in business_keywords):
            tags.add('Business')
        
        # Arts subjects
        arts_keywords = ['art', 'design', 'music', 'creative', 'media', 'film', 'drama', 'literature']
        if any(keyword in content for keyword in arts_keywords):
            tags.add('Arts')
        
        # Health subjects
        health_keywords = ['health', 'medicine', 'nursing', 'therapy', 'medical', 'care']
        if any(keyword in content for keyword in health_keywords):
            tags.add('Health')
        
        # Education
        education_keywords = ['education', 'teaching', 'early learning', 'childcare']
        if any(keyword in content for keyword in education_keywords):
            tags.add('Education')
        
        # Default if no specific tags found
        if not tags:
            tags.add('General')
        
        return ', '.join(sorted(tags))
    
    def standardize_duration(self, duration: str) -> str:
        """Standardize duration format."""
        if not duration:
            return ""
        
        duration = duration.lower().strip()
        
        # Common patterns
        if 'year' in duration:
            years = re.search(r'(\d+)', duration)
            if years:
                num = int(years.group(1))
                return f"{num} Year{'s' if num > 1 else ''}"
        
        if 'month' in duration:
            months = re.search(r'(\d+)', duration)
            if months:
                num = int(months.group(1))
                return f"{num} Month{'s' if num > 1 else ''}"
        
        if 'week' in duration:
            weeks = re.search(r'(\d+)', duration)
            if weeks:
                num = int(weeks.group(1))
                return f"{num} Week{'s' if num > 1 else ''}"
        
        return duration.title()
    
    def generate_keywords(self, course: Dict[str, Any]) -> str:
        """Generate searchable keywords from course data."""
        keywords = set()
        
        # Add words from name
        name = course.get('name', '')
        if name:
            keywords.update(word.lower() for word in re.findall(r'\w+', name) if len(word) > 2)
        
        # Add words from description
        description = course.get('description', '')
        if description:
            keywords.update(word.lower() for word in re.findall(r'\w+', description) if len(word) > 3)
        
        # Add tags
        tags = course.get('tags', '')
        if tags:
            keywords.update(tag.lower() for tag in tags.split(','))
        
        # Remove common stop words
        stop_words = {'the', 'and', 'for', 'are', 'with', 'this', 'that', 'course', 'programme', 'program'}
        keywords = {word for word in keywords if word not in stop_words}
        
        return ', '.join(sorted(keywords))
    
    def log_validation_stats(self):
        """Log validation statistics."""
        stats = self.validation_stats
        logger.info("=== Data Cleaning Statistics ===")
        logger.info(f"Total records processed: {stats['total_records']}")
        logger.info(f"Valid records: {stats['valid_records']}")
        logger.info(f"Duplicates removed: {stats['duplicates_removed']}")
        logger.info(f"Success rate: {(stats['valid_records']/stats['total_records']*100):.1f}%")
        
        if stats['validation_errors']:
            logger.warning(f"Validation errors: {len(stats['validation_errors'])} records failed")
            for course, errors in list(stats['validation_errors'].items())[:5]:
                logger.debug(f"  {course}: {errors}")
    
    def save_processed_data(self, courses: List[Dict[str, Any]], filename: str = "processed_courses.csv"):
        """Save processed data to file."""
        if not courses:
            logger.warning("No courses to save")
            return
        
        filepath = PROCESSED_DATA_DIR / filename
        
        try:
            df = pd.DataFrame(courses)
            df.to_csv(filepath, index=False, encoding='utf-8')
            logger.info(f"Saved {len(courses)} processed courses to {filepath}")
        except Exception as e:
            logger.error(f"Error saving processed data: {e}")


def main():
    """Test the data cleaner with scraped Qualifax data."""
    import json
    
    # Load raw scraped data
    raw_file = Path(__file__).parent.parent / "data" / "raw" / "qualifax_courses_raw.json"
    
    if not raw_file.exists():
        logger.error("Raw data file not found. Run the scraper first.")
        return
    
    with open(raw_file, 'r', encoding='utf-8') as f:
        raw_courses = json.load(f)
    
    logger.info(f"Loaded {len(raw_courses)} raw courses for cleaning")
    
    # Clean the data
    cleaner = CourseDataCleaner()
    cleaned_courses = cleaner.clean_course_data(raw_courses)
    
    # Save processed data
    cleaner.save_processed_data(cleaned_courses, "qualifax_processed.csv")
    
    # Show sample results
    if cleaned_courses:
        logger.info("Sample processed courses:")
        for i, course in enumerate(cleaned_courses[:3]):
            logger.info(f"{i+1}. {course['name']} [{course.get('cao_code', 'No Code')}]")
            logger.info(f"   Tags: {course.get('tags', 'N/A')}")
            logger.info(f"   Keywords: {course.get('keywords', 'N/A')[:100]}...")


if __name__ == "__main__":
    main()