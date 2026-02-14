"""Common utility functions for web scraping."""

import time
import random
from typing import Dict, Any, Optional
from loguru import logger
from fake_useragent import UserAgent
from ratelimit import limits, sleep_and_retry


def get_random_user_agent() -> str:
    """Get a random user agent string."""
    try:
        ua = UserAgent()
        return ua.random
    except Exception:
        # Fallback to a common user agent
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"


@sleep_and_retry
@limits(calls=1, period=2)  # Rate limit: 1 call per 2 seconds
def respectful_delay(base_delay: float = 2.0) -> None:
    """Add a respectful delay between requests with some randomization."""
    # Add 20% randomization to the delay
    delay = base_delay + random.uniform(-0.2 * base_delay, 0.2 * base_delay)
    time.sleep(max(0.5, delay))  # Minimum 0.5 second delay


def clean_text(text: str) -> str:
    """Clean and normalize text data."""
    if not text:
        return ""
    
    # Remove extra whitespace and normalize
    cleaned = " ".join(text.strip().split())
    
    # Remove common unwanted characters
    cleaned = cleaned.replace("\u00a0", " ")  # Non-breaking space
    cleaned = cleaned.replace("\u2019", "'")  # Right single quotation mark
    cleaned = cleaned.replace("\u201c", '"')  # Left double quotation mark
    cleaned = cleaned.replace("\u201d", '"')  # Right double quotation mark
    
    return cleaned.strip()


def extract_year_from_text(text: str) -> Optional[int]:
    """Extract a year from text (typically 2020-2030 range)."""
    import re
    
    if not text:
        return None
    
    # Look for 4-digit years in reasonable range
    years = re.findall(r'\b(20[2-3]\d)\b', text)
    if years:
        return int(years[0])
    
    return None


def normalize_course_code(code: str) -> str:
    """Normalize course codes to consistent format."""
    if not code:
        return ""
    
    # Remove extra spaces and convert to uppercase
    normalized = code.strip().upper()
    
    # Common patterns: AB123, AB1234, etc.
    import re
    match = re.search(r'([A-Z]{2,4})(\d{3,4})', normalized)
    if match:
        return f"{match.group(1)}{match.group(2)}"
    
    return normalized


def safe_get_attribute(element, attribute: str, default: str = "") -> str:
    """Safely get an attribute from a web element."""
    try:
        value = element.get_attribute(attribute)
        return clean_text(value) if value else default
    except Exception as e:
        logger.debug(f"Error getting attribute {attribute}: {e}")
        return default


def safe_get_text(element, default: str = "") -> str:
    """Safely get text content from a web element."""
    try:
        text = element.text or element.get_attribute("textContent")
        return clean_text(text) if text else default
    except Exception as e:
        logger.debug(f"Error getting text: {e}")
        return default


def create_course_record(
    cao_code: str = "",
    name: str = "",
    description: str = "",
    nfq_level: int = 8,
    duration: str = "",
    tags: str = "",
    course_url: str = "",
    college_id: int = 1,
    points: int = 0,
    entry_requirements: str = "",
    career_info: str = ""
) -> Dict[str, Any]:
    """Create a standardized course record dictionary."""
    return {
        "cao_code": normalize_course_code(cao_code),
        "name": clean_text(name),
        "description": clean_text(description),
        "nfq_level": nfq_level,
        "duration": clean_text(duration),
        "tags": clean_text(tags),
        "course_url": course_url,
        "college_id": college_id,
        "points": points,
        "entry_requirements": clean_text(entry_requirements),
        "career_info": clean_text(career_info),
        "created_at": "2025-01-25T15:00:00"
    }