# Define functions that our model can use
# 20260812 jmd first version
# 20261009 add decline()
# 20261012 add percent functions

def add(x: float, y: float):
    """
    Adds two numbers together (sum, total, plus).

    Args:
        x: the first number
        y: the second number

    Returns:
        result: the sum of x and y.
    """
    return {"result": x + y}

def subtract(x: float, y: float):
    """
    Subtracts one number from another (difference, minus).

    Args:
        x: the starting number
        y: the number to subtract from x

    Returns:
        result: the difference between x and y (x minus y).
    """
    return {"result": x - y}

def multiply(x: float, y: float):
    """
    Multiplies two numbers together (product, times).

    Args:
        x: the first number
        y: the second number

    Returns:
        result: the product of x and y.
    """
    return {"result": x * y}

def divide(x: float, y: float):
    """
    Divides one number by another (quotient, over).

    Args:
        x: the numerator
        y: the denominator

    Returns:
        result: the quotient of x and y (x over y).
    """
    return {"result": x / y}

def add_percent(x: float, y: float):
    """
    Increases a number by a percentage (markup, tax added, raise, increase, up by).

    Args:
        x: the starting number
        y: the percent to add, as written (30 for 30%)

    Returns:
        result: x increased by the percent (x plus y% of x).
    """
    return {"result": x * (1 + y / 100)}

def subtract_percent(x: float, y: float):
    """
    Decreases a number by a percentage (discount, off, drop, reduction, down by).

    Args:
        x: the starting number
        y: the percent to subtract, as written (30 for 30%)

    Returns:
        result: x decreased by the percent (x minus y% of x).
    """
    return {"result": x * (1 - y / 100)}

def percent_of(x: float, y: float):
    """
    Calculates a percentage of a number (20% of 2300, the tax amount, the share).

    Args:
        x: the number to take a percentage of
        y: the percent to take, as written (20 for 20%)

    Returns:
        result: y% of x.
    """
    return {"result": x * y / 100}

def decline(reason: str = "not given"):
    """
    Declines a request that is not an add, subtract, multiply, divide, or percent
    calculation on two numbers (abstain, cannot answer, not a calculation).

    Args:
        reason: a few words on why, in the language of the request (for example
            "small talk" or "闲聊")

    Returns:
        reason: the reason the request was declined.
    """
    return reason

tools = [
    add, subtract, multiply, divide,
    add_percent, subtract_percent, percent_of,
    decline,
]
