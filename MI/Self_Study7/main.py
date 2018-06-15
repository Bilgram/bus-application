import pandas as pd
import numpy as np
import time
from sklearn import preprocessing

max_user_id = 49
number_of_users = max_user_id + 1

def cos_sim(a, b): #https://masongallo.github.io/machine/learning,/python/2016/07/29/cosine-similarity.html
    dot_product = np.dot(a, b)
    norm_a = np.linalg.norm(a)
    norm_b = np.linalg.norm(b)

    return dot_product / (norm_a * norm_b)

def get_edges():
    edges = pd.read_csv('Brightkite_edges.txt', sep='\t', names=['user1', 'user2'])
    return edges

def get_checkins():
    checkins = pd.read_csv('Brightkite_totalCheckins.txt', sep='\t', names=['user', 'check-in time', 'latitude', 'longitude', 'location id'])
    return checkins

def get_reshaped(column1, column2):
    reshaped = pd.crosstab(column1, column2)
    return reshaped

def normalize_dataframe(df):
    return (df.T/df.T.sum()).T

def create_f_adj_matrix():
    user_edges = get_edges()
    subset_user_edges = user_edges.head(10717) #10717 is the number of rows equal to 109 users
    subset_user_edges = subset_user_edges[subset_user_edges['user2'] <= max_user_id]
    adj_matrix_user1_user2 = get_reshaped(subset_user_edges['user1'], subset_user_edges['user2'])

    #d = subset_user_edges.groupby(['user1']).size() #Used if we create PPR using only F. If PPR with only F is to be used, then d must be parsed and used when calculating b[col] in personal_pagerange()

    return adj_matrix_user1_user2

def create_fl_adj_matrix():
    user_event = get_checkins() #removed a lot of unused data to increase speed
    subset_user_event = user_event[(user_event['user'] <= max_user_id) & (user_event['longitude'] != 0.0)]
    adj_matrix_user_locations = get_reshaped(subset_user_event['user'], subset_user_event['location id'])

    return normalize_dataframe(adj_matrix_user_locations)

def personal_pagerank(u, p):
    pi = np.zeros(number_of_users)
    b = np.zeros(number_of_users)
    b_prev = np.zeros(number_of_users)

    b[u] = 1
    epsilon = 0.00000001
    alpha = 0.85

    while(not np.array_equal(b, b_prev)): #TODO hvorfor konvergerer vi?
        b_prev = np.copy(b)
        for i in range(0, number_of_users):
            if b[i] < epsilon:
                continue
            pi[i] = pi[i] + (1 - alpha) * b[i]
            for col, val in p[i].iteritems():
                if val is not 0: #has to be friend
                    #b[col] = b[col] + alpha * b[i] / d[i] #even distribution of friendship-weight
                    b[col] = b[col] + alpha * val

            b[i] = 0

    return pi

def create_sim_matrix(matrix):
    sim_matrix = np.zeros([number_of_users, number_of_users])

    for user1 in range(0, max_user_id):
        for user2 in range(0, max_user_id):
            sim_matrix[user1, user2] = cos_sim(matrix.iloc[user1], matrix.iloc[user2])

    return sim_matrix

def create_p():
    adj_matrix_fl = create_fl_adj_matrix() #dataframe
    adj_matrix_f = create_f_adj_matrix() #dataframe
    p = np.zeros([number_of_users, number_of_users])
    beta = 0.85
    sim_matrix = create_sim_matrix(adj_matrix_fl)

    for user1 in range(0, max_user_id): #108 is never calculated
        number_of_friends = np.count_nonzero(adj_matrix_f[user1])
        s_u = np.sum(sim_matrix[user1])
        for user2 in range(0, max_user_id):
            if adj_matrix_f.iloc[user1,user2] == 1 and cos_sim(adj_matrix_fl.iloc[user1],adj_matrix_fl.iloc[user2]) != 0:
                p[user1, user2] = ((1 - beta) / number_of_friends) + (beta / s_u) * sim_matrix[user1, user2]
            elif (adj_matrix_f.iloc[user1,user2] == 1) :
                p[user1,user2] = (1-beta) * (1 / number_of_friends)
            elif cos_sim(adj_matrix_fl.iloc[user1],adj_matrix_fl.iloc[user2]) != 0:
                p[user1, user2] = (beta / s_u) * sim_matrix[user1, user2]
            else:
                continue

    np.fill_diagonal(p, 0)
    df_p = pd.DataFrame(p)
    return normalize_dataframe(df_p)

def get_pi():
    p = create_p()

    pi = np.zeros([number_of_users, number_of_users])

    for user in range(0, max_user_id):
        pi[user] = personal_pagerank(user, p)

    return pi

def LFBCA(ppr, u, N, adj_matrix_fl):
    df_fl = pd.DataFrame(adj_matrix_fl)
    row = df_fl.iloc[u]
    sl = row[row == 0]

    for friend in range(0, max_user_id):
        if friend == u:
            continue
        for location, val in df_fl.iloc[friend].iteritems():
            if val != 0 and location in sl.index:
                sl.loc[location] = sl.loc[location] + ppr[u, friend] * val

    ordered = sl.sort_values(ascending=False).head(N)

    return ordered

def get_LFBCA(N):
    LFBCA_all = np.zeros([number_of_users, N])
    ppr = get_pi() #ppr = np.loadtxt('ppr.out')
    adj_matrix_fl = create_fl_adj_matrix()

    for user in range(0, max_user_id):
        print(str(user) + ' Flækkes')
        LFBCA_all[user] = LFBCA(ppr, user, N, adj_matrix_fl)

    return LFBCA_all

if __name__ == '__main__':
    #ppr = get_pi()
    #np.savetxt('ppr.out', ppr)

    res = get_LFBCA(10)
    np.savetxt('LFBCA.out', res)
    print(res)

    #test = pi.sum(axis=1)
    #print('Der flækkes')

    #sentence_m = np.array([1, 1, 2])
    #sentence_h = np.array([0, 1, 1])
    #print(cos_sim(sentence_m, sentence_h))