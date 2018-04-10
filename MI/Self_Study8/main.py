from DAT8.MI.Self_Study7 import main
import numpy as np
import itertools
import pandas as pd

data = main.get_edges()
subset_edges = data.head(877)

def sim_rank(adj_matrix, iterations):
    identity_matrix = np.identity(len(adj_matrix.index))
    result_matrix = np.zeros((len(adj_matrix.index), len(adj_matrix.index)))

    for i in range(0,iterations):
        for (x,y) in np.ndenumerate(result_matrix):
            result_matrix[x[0],x[1]] = sim_rank_helper(x[0],x[1],0.8,adj_matrix, identity_matrix)

        identity_matrix = result_matrix

    return result_matrix

def sim_rank_helper(a, b, C, adj_matrix, identity_matrix):
    if a is b:
        return 1
    elif (a is None) or (b is None):
        return 0
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

        for comb in combined:
            total_comb_value += identity_matrix[comb[0], comb[1]]

        return total_comb_value*normalized_factor

if __name__ == '__main__':
    adj_matrix = main.get_reshaped(subset_edges['user1'], subset_edges['user2'])
    iterations = 10
    result = sim_rank(adj_matrix, iterations)

    df_for_visuals = pd.DataFrame(result)
    print(df_for_visuals)