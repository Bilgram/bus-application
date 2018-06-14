import wikipedia
from bs4 import BeautifulSoup as bs
import pandas as pd
import numpy as np

content_size = 500
cities = ['New York City', 'London', 'Singapore', 'Angelsberg', 'Hong Kong', 'Los Angeles', 'Paris', 'Chicago',
'Washington, D.C.', 'San Francisco', 'Rome', 'Mumbai', 'Toronto', 'Philadelphia', 'Monaco', 'Amsterdam', 'Tokyo',
'Boston', 'Barcelona', 'Istanbul', 'Berlin', 'Pompeii', 'Rio de Janeiro', 'Seattle', 'Venice', 'Sparta', 'Shanghai',
'Vancouver', 'Jerusalem', 'Macau', 'Montreal']
adjacency_list = np.zeros(shape=(len(cities), len(cities)))

def get_text(name):
    page = wikipedia.page(name)

    return page.content[0:content_size], page.html()

def get_links(html):
    soup = bs(html, "html.parser")
    hlinks = []
    for link in soup.find_all("a"):
        url = link.get("href", "")
        if "/wiki/" in url:
            hlinks.append(link.text.strip())

    return hlinks

for i, city1 in enumerate(cities):
    print(i)
    content, html = get_text(city1)
    links = get_links(html)
    for j, city2 in enumerate(cities):
        content2 = get_text(city2)
        if any(link in content2 for link in links):
            adjacency_list[i,j] = 1
            print(adjacency_list)

#https://github.com/goldsmith/Wikipedia/issues/35