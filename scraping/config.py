"""Configuration settings for CAO course data scraping."""

import os
from pathlib import Path

# Base paths
PROJECT_ROOT = Path(__file__).parent
DATA_DIR = PROJECT_ROOT / "data"
RAW_DATA_DIR = DATA_DIR / "raw"
PROCESSED_DATA_DIR = DATA_DIR / "processed"
OUTPUT_DATA_DIR = DATA_DIR / "output"

# Create directories if they don't exist
for dir_path in [DATA_DIR, RAW_DATA_DIR, PROCESSED_DATA_DIR, OUTPUT_DATA_DIR]:
    dir_path.mkdir(exist_ok=True)

# Scraping settings
SCRAPING_CONFIG = {
    "delay_between_requests": 2.0,  # seconds
    "timeout": 30,  # seconds
    "max_retries": 3,
    "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "headless": True,  # for Playwright/Selenium
}

# Data sources
SOURCES = {
    "qualifax": {
        "base_url": "https://www.qualifax.ie",
        "enabled": True,
        "priority": 1,
    },
    "cao": {
        "base_url": "https://www.cao.ie",
        "enabled": True,
        "priority": 2,
    },
    "colleges": {
        "enabled": False,  # Phase 3
        "priority": 3,
    }
}

# Existing data file
EXISTING_DATA_FILE = PROJECT_ROOT.parent / "src/main/resources/data/cao_courses_db.csv"

# Output settings
OUTPUT_CONFIG = {
    "csv_encoding": "utf-8",
    "csv_delimiter": ",",
    "include_timestamp": True,
}

# Data validation rules
VALIDATION_RULES = {
    "required_fields": ["name", "nfq_level"],  # CAO code not required for all courses
    "min_description_length": 5,  # More lenient
    "valid_nfq_levels": [5, 6, 7, 8, 9, 10],
    "max_course_name_length": 200,
    "min_course_name_length": 3,
}

# Logging configuration
LOGGING_CONFIG = {
    "level": "INFO",
    "format": "{time:YYYY-MM-DD HH:mm:ss} | {level} | {message}",
    "rotation": "1 MB",
    "retention": "7 days",
}