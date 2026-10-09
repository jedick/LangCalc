# Define functions that our model can use

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

def abstain():
    """
    Declines a request that is not a calculation (unknown, not math, cannot answer).
    Call this when the user does not ask to add, subtract, multiply, or divide two numbers:
    general knowledge questions, small talk, nonsense, an unsupported language, or a
    request where a number is missing.

    Returns:
        message: a statement that the request cannot be answered.
    """
    return {"message": "Sorry, I can only add, subtract, multiply, or divide two numbers."}

tools = [add, subtract, multiply, divide, abstain]

