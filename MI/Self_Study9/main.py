import wikipedia
from bs4 import BeautifulSoup as bs
import pandas as pd
import numpy as np
from collections import Counter
import math
import csv

#Rel-RW (Robust EL with RandomWalks)

modern_political_leaders_entity = ['Donald Trump','Barack Obama','Adolf Hitler','Elizabeth II','Abraham Lincoln',
                             'John F. Kennedy','Henry VIII of England','Nelson Mandela','George W. Bush',
                             'Ronald Reagan','Queen Victoria','Bill Clinton','George Washington','Osama bin Laden',
                             'Franklin D. Roosevelt','Elizabeth I','Winston Churchill','Mahatma Gandhi',
                             'Christopher Columbus','Che Guevara','Joseph Stalin','Richard Nixon','Vladimir Putin',
                             'Charles, Prince of Wales','Prince Philip, Duke of Edinburgh','Sarah Palin',
                             'Theodore Roosevelt','Thomas Jefferson','Benjamin Franklin','George H.W. Bush',
                             'Margaret Thatcher']

def get_html(name):
    return wikipedia.page(name).html()

def get_links_mention(html):
    soup = bs(html, "html.parser")
    hlinks = []
    for element in soup.find_all("a"):
        url = element.get('href')
        if url is not None and "/wiki/" in url:
            hlinks.append(url)

    return hlinks

# def create_semantic_signature(entities):
#     wiki_entities = list(map(lambda x: ('/wiki/') + x.replace(' ', '_'), entities))
#     semantic_signature = pd.DataFrame(columns=wiki_entities)
#
#     for i, entity in enumerate(entities):
#         print(entity)
#         html = get_html(entity)
#         links = get_links_mention(html)
#
#         occurences = Counter([l for l in links if l in wiki_entities]) #Returns dict with political leader occurences
#         occurences = pd.Series(occurences, name=entity)
#         count = len(occurences)
#         occurences = occurences.divide(count)
#
#         semantic_signature = semantic_signature.append(occurences)
#         semantic_signature = semantic_signature.fillna(0)
#
#     return semantic_signature

def create_semantic_signature(entity, links, links_checker):
    occurences = Counter([l for l in links if l in links_checker])  # Returns dict with political leader occurences
    occurences = pd.Series(occurences, name=entity)
    count = occurences.sum()
    occurences = occurences.divide(count)

    return occurences

def create_semantic_signature_document(wiki_page):
    entities = get_links_mention(get_html(wiki_page))
    wiki_entities = list(map(lambda x: ('/wiki/') + x.replace(' ', '_'), modern_political_leaders_entity))

    entity_probs = create_semantic_signature(wiki_page, entities, wiki_entities)

    return entity_probs

def create_semantic_signature_list(entities):
    wiki_entities = list(map(lambda x: ('/wiki/') + x.replace(' ', '_'), entities))
    semantic_signature = pd.DataFrame(columns=wiki_entities)

    for i, entity in enumerate(entities):
        links = get_links_mention(get_html(entity))
        occurences = create_semantic_signature(entity, links, wiki_entities)
        semantic_signature = semantic_signature.append(occurences)
        semantic_signature = semantic_signature.fillna(0)

    return semantic_signature

def create_wiki_term(t):
    return ('/wiki/') + t.replace(' ', '_')

def tf(term, mentions):
    frequencies = Counter(mentions)
    frequencies = pd.Series(frequencies)
    max = frequencies.max()

    try:
        ftd = frequencies[term]
    except KeyError:
        ftd = 1 # add one  for the first (non hyperlink) mention

    return 0.5+0.5*(ftd/max)

def idf_func(term, D):
    N = len(D)

    wterm = create_wiki_term(term)
    numDocs = 1  # 1 for avoidig division by zero
    for d in D.values():
        if wterm in d:
            numDocs += 1

    #numDocs = len([d for d in D if (wterm in d)])
    division = N/numDocs
    log = math.log(division)
    print(type(log))
    return log

def tfidf(t, d, D):
    tf_val = tf(t,d)
    idf_val = idf_func(t, D)
    return tf_val*idf_val

def create_D():
    D = {}
    for leader in modern_political_leaders_entity:
        print(leader)
        D[leader] = (get_links_mention(get_html(leader)))

    return D

if __name__ == '__main__':
    #D = pd.Series.from_csv('document_corpus.csv')
    D = create_D()
    #s = create_semantic_signature(modern_political_leaders_entity)
    #s = create_semantic_signature_document('List_of_current_heads_of_state_and_government')
    #print(s)

    idf = idf_func('Donald Trump', D)
    #t = tf(modern_political_leaders_entity[0], ['Donald Trump', 'Donald Trump', 'Daniel', 'Daniel', 'Daniel', 'Daniel', 'Bille'])
    d = D['Donald Trump']
    tfidfvar = tfidf('Donald Trump', d, D)

    importances = list(map((lambda x : tfidf(x, D[x], D)), modern_political_leaders_entity))


    #missing pof what ever
    print(importances)
    #semantic_signature = pd.read_csv('semantic_signature.csv', index_col=0)
