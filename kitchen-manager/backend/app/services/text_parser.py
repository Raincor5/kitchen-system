import difflib
import re
from datetime import datetime
import logging
from typing import Dict, Any, List

logger = logging.getLogger(__name__)

def find_closest_match(input_string: str, candidates: List[str]) -> str:
    """Find the closest match for a string from a list of candidates."""
    closest_match = difflib.get_close_matches(input_string, candidates, n=1, cutoff=0.6)
    return closest_match[0] if closest_match else ""

def extract_dates_with_details(lines: List[str]) -> List[str]:
    """Extract dates while preserving time and additional info like EOD."""
    date_pattern = r"(\d{1,2}/\d{1,2}/\d{2}(?:\s+\d{2}:\d{2})?(?:\s+[A-Za-z.\s]+)?)"
    dates = []
    for line in lines:
        matches = re.findall(date_pattern, line)
        dates.extend([match.strip() for match in matches])
    return dates

def extract_batch_number(lines: List[str]) -> str:
    """Extract batch number using flexible matching."""
    batch_pattern = r"(Batch No[:\s]*)([^\n]+)"
    for line in lines:
        match = re.search(batch_pattern, line, re.IGNORECASE)
        if match:
            batch_no = match.group(2).strip()
            # Ensure batch number has at least 2 characters
            if len(batch_no) < 2:
                return "N/A"
            return batch_no
    return "N/A"

def parse_label_text(text: str, product_names: List[str], employee_names: List[str]) -> Dict[str, Any]:
    """Parse and extract data from label text."""
    lines = [line.strip() for line in text.split("\n") if line.strip()]

    # Extract product name and RTE type
    product_name = ""
    rte_status = ""
    for line in lines:
        closest_product = find_closest_match(line, product_names)
        if closest_product:
            product_name = closest_product
            rte_status = "RTE" if "RTE" in line else ""
            break

    # Identify label type
    label_type = "Defrosted" if any("DEFROST" in line.upper() for line in lines) else "Normal"

    # Extract employee name
    employee_name = ""
    for line in lines:
        closest_employee = find_closest_match(line, employee_names)
        if closest_employee:
            employee_name = closest_employee
            break

    # Extract dates
    extracted_dates = extract_dates_with_details(lines)

    # Extract batch number
    batch_no = extract_batch_number(lines)

    # Find day of the week for the expiry date
    expiry_date = None
    expiry_day = "N/A"
    if extracted_dates:
        try:
            expiry_date_str = extracted_dates[-1].split()[0]  # Take only the date part
            expiry_date = datetime.strptime(expiry_date_str, "%d/%m/%y")
            expiry_day = expiry_date.strftime("%A").upper()
        except ValueError:
            pass  # Ignore invalid date formats

    return {
        "product_name": product_name,
        "rte_status": rte_status,
        "employee_name": employee_name,
        "label_type": label_type,
        "dates": extracted_dates,
        "batch_no": batch_no,
        "expiry_day": expiry_day
    } 