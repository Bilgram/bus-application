import pandas as pd
import numpy as np
import time

def get_edges():
    edges = pd.read_csv('Brightkite_edges.txt', sep='\t', names=['user1', 'user2'])
    return edges

def get_checkins():
    checkins = pd.read_csv('Brightkite_totalCheckins.txt', sep='\t', names=['user', 'check-in time', 'latitude', 'longitude', 'location id'])
    return checkins

def get_reshaped(column1, column2):
    reshaped = pd.crosstab(column1, column2)
    return reshaped

def personal_pagerank(adj_matrix, number_of_occurence):
    size = len(adj_matrix.index)
    pi = np.zeros(size)
    b = np.zeros(size)
    b_prev = np.zeros(size)
    d = number_of_occurence

    u = 0
    b[u] = 1
    epsilon = 1
    alpha = 0.85

    print('Der flækkes!')
    first_time = time.time()
    while (b_prev is not b):
        for i in range(0, size):
            if b[i] < epsilon:
                continue
            pi[i] = pi[i] + (1 - alpha) * b[i]
            b_prev = b
            for col, val in adj_matrix[i].iteritems():
                if val is 1:
                    b[col] = b[col] + alpha * b[i] / d[i]

    duration = time.time() - first_time

    print('Execution time:', duration)

    return pi

if __name__ == '__main__':
    edges = get_edges()
    subset_edges = edges.head(10717) #10717 is the number of rows equal to 109 users
    reshaped = get_reshaped(subset_edges['user1'], subset_edges['user2'])

    d = subset_edges.groupby(['user1']).size()

    pi = personal_pagerank(reshaped, d)
    print(pi)

    np.savetxt('output.out', pi)