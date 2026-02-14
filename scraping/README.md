# CAO Course Data Scraping

This directory contains scripts for scraping and improving Irish college course data from multiple sources.

## Data Sources

1. **Qualifax.ie** - Comprehensive course database with detailed descriptions
2. **CAO.ie** - Official points and entry requirements
3. **Individual College Websites** - Enhanced course details and career information

## Project Structure

```
scraping/
├── README.md                 # This file
├── requirements.txt          # Python dependencies
├── config.py                 # Configuration settings
├── scrapers/
│   ├── __init__.py
│   ├── qualifax_scraper.py   # Qualifax.ie course data
│   ├── cao_scraper.py        # CAO.ie points and requirements
│   └── college_scrapers.py   # Individual college websites
├── utils/
│   ├── __init__.py
│   ├── data_cleaner.py       # Data cleaning and validation
│   ├── merger.py             # Merge with existing data
│   └── helpers.py            # Common utility functions
├── data/
│   ├── raw/                  # Raw scraped data
│   ├── processed/            # Cleaned and processed data
│   └── output/               # Final merged datasets
└── main.py                   # Main execution script
```

## Usage

1. Install dependencies: `pip install -r requirements.txt`
2. Configure settings in `config.py`
3. Run scraper: `python main.py`

## Data Flow

Raw scraped data → Cleaning/validation → Merge with existing → Output to CSV → Import to backend

## Ethical Considerations

- Respects robots.txt files
- Implements delays between requests
- Uses User-Agent headers appropriately
- Complies with website terms of service