"""
General utility helpers.
"""
import random
import string
from datetime import datetime


def generate_booking_reference() -> str:
    """Generate a unique booking reference like CPX-20240101-A3B7."""
    date_part = datetime.utcnow().strftime("%Y%m%d")
    random_part = "".join(random.choices(string.ascii_uppercase + string.digits, k=4))
    return f"CPX-{date_part}-{random_part}"


def generate_ticket_code() -> str:
    """Generate a unique ticket code."""
    random_part = "".join(random.choices(string.ascii_uppercase + string.digits, k=10))
    return f"TKT-{random_part}"
