import pandas as pd

try:
    df_items = pd.read_excel('/Users/christambayong/Downloads/20260616 - ITEM OUTLET.xlsx', sheet_name='ITEM')
    print("Items found:", len(df_items))
    print(df_items.head(10))
    
    df_outlets = pd.read_excel('/Users/christambayong/Downloads/20260616 - ITEM OUTLET.xlsx', sheet_name='OUTLET')
    print("Outlets found:", len(df_outlets))
    print(df_outlets.head())
except Exception as e:
    print("Error:", e)
