

with open('document1', 'r', encoding='utf8') as f:
    document1 = f.readlines()

for line in document1:
    document1_parsed = line.split('. ')

with open('document2', 'r', encoding='utf8') as f:
    document2 = f.readlines()

for line in document2:
    document2_parsed = line.split('. ')

print(document2_parsed)