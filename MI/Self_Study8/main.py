from DAT8.MI.Self_Study7 import main
import numpy as np
import itertools
import pandas as pd

def cos_sim(a, b): #https://masongallo.github.io/machine/learning,/python/2016/07/29/cosine-similarity.html
    dot_product = np.dot(a, b)
    norm_a = np.linalg.norm(a)
    norm_b = np.linalg.norm(b)

    return dot_product / (norm_a * norm_b)

def cos_sim_matrix(adj_matrix):
    result_matrix_cos = np.zeros((len(adj_matrix.index), len(adj_matrix.index)))

    rows = adj_matrix.shape[0]
    cols = adj_matrix.shape[1]

    for row in range(rows):
        for col in range(cols):
            result_matrix_cos[row,col] = cos_sim(adj_matrix[row], adj_matrix[col])

    return result_matrix_cos

def sim_rank(adj_matrix, iterations):
    identity_matrix = np.identity(len(adj_matrix.index))
    result_matrix = np.zeros((len(adj_matrix.index), len(adj_matrix.index)))
    C = 0.8

    for i in range(0,iterations):
        for (x,y) in np.ndenumerate(result_matrix):
            result_matrix[x[0],x[1]] = sim_rank_helper(x[0],x[1],C,adj_matrix, identity_matrix)

        identity_matrix = result_matrix

    return identity_matrix

def sim_rank_helper(a, b, C, adj_matrix, identity_matrix):
    if a is b:
        return 1
    # Should be added if the other method is used
    # elif (a is None) or (b is None): #with our data we will never enter this. We said that a pair is friends if one of them is conneceted(we have no directions).
    #     return 0
    else:
        a_neighbours = []
        b_neighbours = []
        total_comb_value = 0
        for col, val in adj_matrix[a].iteritems():
            if val is 1:
                a_neighbours.append(col)
        for col, val in adj_matrix[b].iteritems():
            if val is 1:
                b_neighbours.append(col)
        combined = list(itertools.product(a_neighbours, b_neighbours))
        normalized_factor = C/len(combined)

        #This is done when an iteration approach is used.
        for comb in combined:
            total_comb_value += identity_matrix[comb[0], comb[1]]

        # This(recursion) could be done if the graph was simplified(symmetric pairs are ommitted).
        # for comb in combined:
        #     total_comb_value += sim_rank_helper(comb[0], comb[1], C, adj_matrix, identity_matrix)

        return normalized_factor * total_comb_value

if __name__ == '__main__':
    data = main.get_edges()
    number_of_users = 6
    subset_edges = data.head(877)
    subset_edges = subset_edges[(subset_edges['user1'] < number_of_users) & (subset_edges['user2'] < number_of_users)]

    adj_matrix = main.get_reshaped(subset_edges['user1'], subset_edges['user2'])
    iterations = 50 #Could be done using while (no new change)
    result_sim_rank = sim_rank(adj_matrix, iterations)
    result_cos_rank = cos_sim_matrix(adj_matrix)
    print(result_sim_rank)