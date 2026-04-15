import os

def check_brackets(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    stack = []
    lines = content.split('\n')
    for i, line in enumerate(lines):
        for char in line:
            if char == '{':
                stack.append(('{', i + 1))
            elif char == '}':
                if not stack:
                    print(f"Extra closing brace at line {i + 1}")
                    return
                stack.pop()
    
    if stack:
        for char, line in stack:
            print(f"Unclosed open brace at line {line}")

check_brackets('c:/Users/ASUS/Desktop/agriSmartJava/src/main/resources/css/style.css')
