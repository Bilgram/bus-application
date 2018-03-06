import pandas as pd

columns = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24']
df = pd.read_csv(r'1518_VPC_MICROLOG.9031280014603051.11-1-2016 13_56_40_243.csv', sep=';', skiprows=3, names=columns)

df = df[df['2'] != 'AIR']

writer = pd.ExcelWriter('output.xlsx')
df.to_excel(writer, 'Ark1')
writer.save()
print(df)