import pandas as pd
import time

#ratings = pd.read_csv(r'C:\Development\DAT8\MI\Self_Study5\ml-latest-small\ratings.csv')
#movies = pd.read_csv(r'C:\Development\DAT8\MI\Self_Study5\ml-latest-small\movies.csv')

def create_p(row):
    return ratings.loc[ratings['userId'] == row['userId'], 'rating'].sum()

start_time = time.time()
#ratings['mean'] = ratings['rating']/ratings.apply(create_p, axis=1)
#ratings.to_csv('ratings.csv')

ratings = (pd.read_csv('ratings.csv')).head(300)
print(ratings.transpose())

df = pd.DataFrame(columns=ratings['movieId'])

for i, row in ratings.iterrows():
    df.loc[i,row['movieId']] = row['mean']

print(df)
print("--- %s seconds ---" % (time.time() - start_time))