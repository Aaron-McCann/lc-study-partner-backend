#!/usr/bin/env python3
import csv

# Paper mapping: {(year, type): paper_id}
PAPERS = {
    (2025, "mock"): 1,
    (2024, "state"): 2,
    (2024, "mock"): 3,
    (2023, "state"): 4,
    (2023, "deferred"): 5,
    (2023, "mock"): 6,
    (2022, "state"): 7,
    (2022, "deferred"): 8,
    (2022, "mock"): 9,
    (2021, "mock"): 10,
    (2020, "state"): 11,
    (2020, "mock"): 12,
    (2019, "state"): 13,
    (2019, "mock"): 14,
    (2018, "state"): 15,
    (2017, "state"): 16,
    (2017, "mock"): 17,
    (2016, "state"): 18,
    (2015, "state"): 19,
    (2014, "state"): 20,
    (2014, "mock"): 21,
    (2013, "state"): 22,
    (2013, "mock"): 23,
    (2012, "state"): 24,
    (2012, "mock"): 25,
    (2011, "state"): 26,
    (2010, "state"): 27,
    (2009, "state"): 28,
    (2009, "mock"): 29,
    (2008, "state"): 30,
    (2007, "state"): 31,
    (2006, "state"): 32,
    (2005, "state"): 33,
}

def get_year_from_content(content):
    """Extract year from question content"""
    if "2025" in content:
        return 2025
    elif "2024" in content:
        return 2024
    elif "2023" in content:
        return 2023
    elif "2022" in content:
        return 2022
    elif "2021" in content:
        return 2021
    elif "2020" in content:
        return 2020
    elif "2019" in content:
        return 2019
    elif "2018" in content:
        return 2018
    elif "2017" in content:
        return 2017
    elif "2016" in content:
        return 2016
    elif "2015" in content:
        return 2015
    elif "2014" in content:
        return 2014
    elif "2013" in content:
        return 2013
    elif "2012" in content:
        return 2012
    elif "2011" in content:
        return 2011
    elif "2010" in content:
        return 2010
    elif "2009" in content:
        return 2009
    elif "2008" in content:
        return 2008
    elif "2007" in content:
        return 2007
    elif "2006" in content:
        return 2006
    elif "2005" in content:
        return 2005
    return 2024  # default

def get_source_type(source):
    """Normalize source type"""
    if source.lower() == "mock":
        return "mock"
    elif source.lower() == "state":
        return "state"
    else:
        return "state"  # deferred is still state

def get_correct_paper_id(content, source):
    """Get correct paper ID based on content and source"""
    year = get_year_from_content(content)
    source_type = get_source_type(source)
    
    # Handle deferred papers
    if "Deferred" in content:
        source_type = "deferred"
    
    # Try to find exact match
    paper_id = PAPERS.get((year, source_type))
    if paper_id:
        return paper_id
    
    # Fallback to any available paper for that year
    for (p_year, p_type), p_id in PAPERS.items():
        if p_year == year:
            return p_id
    
    # Ultimate fallback
    return 2

# Read and fix questions
questions = []
with open('data/questions.csv', 'r') as f:
    reader = csv.reader(f)
    for row in reader:
        if len(row) >= 11:
            question_id = row[0]
            old_paper_id = row[1]
            question_number = row[2]
            content = row[3]
            question_link = row[4]
            marks = row[5]
            topic = row[6]
            difficulty = row[7]
            source = row[8]
            sample_answer = row[9]
            created_at = row[10]
            
            # Get correct paper ID
            new_paper_id = get_correct_paper_id(content, source)
            
            # Update question link with real exam paper URLs
            year = get_year_from_content(content)
            if source.lower() == "state" or "Deferred" in content:
                # Use real state exam URL pattern
                new_question_link = f"https://www.examinations.ie/archive/exampapers/{year}/LC032ALP000EV.pdf"
                # Add page anchor based on section
                if "Section 1" in content:
                    new_question_link += "#page=5"
                elif "Section 2" in content:
                    new_question_link += "#page=10"
                elif "Section 3" in content or "Section C" in content:
                    new_question_link += "#page=15"
            else:
                # Mock papers - no real links available
                new_question_link = ""
            
            # Update the row
            new_row = [question_id, new_paper_id, question_number, content, new_question_link, 
                      marks, topic, difficulty, source, sample_answer, created_at]
            questions.append(new_row)
            
            print(f"Question {question_id}: {content[:50]}... -> Paper {new_paper_id}")

# Write fixed questions
with open('data/questions_fixed.csv', 'w', newline='') as f:
    writer = csv.writer(f)
    for row in questions:
        writer.writerow(row)

print(f"\nFixed {len(questions)} questions. Review data/questions_fixed.csv")