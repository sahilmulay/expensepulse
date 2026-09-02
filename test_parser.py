import re
import json

# Extracting the exact transactions from the user's 11 pages of GPay statement
statement_text = """
01 Aug, 2026
04:15 PM
Paid to SRI SAI TIFFANS
UPI Transaction ID: 621324601075
Paid by State Bank of India 7067
₹20

01 Aug, 2026
09:29 PM
Paid to C KAVITHA
UPI Transaction ID: 621350408347
Paid by State Bank of India 7067
₹1,565

02 Aug, 2026
08:36 AM
Paid to Amruta Mote
UPI Transaction ID: 621461611965
Paid by State Bank of India 7067
₹250

02 Aug, 2026
08:40 AM
Paid to Ashna Kumbhar
UPI Transaction ID: 621461739919
Paid by State Bank of India 7067
₹700

02 Aug, 2026
08:42 AM
Paid to Kedar Patil
UPI Transaction ID: 621461818398
Paid by State Bank of India 7067
₹291

02 Aug, 2026
08:44 AM
Paid to Amruta Mote
UPI Transaction ID: 621461960737
Paid by State Bank of India 7067
₹610

02 Aug, 2026
08:55 AM
Received from Kedar Patil
UPI Transaction ID: 621462470740
Paid to State Bank of India 7067
₹64

02 Aug, 2026
11:35 AM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110344850104
Paid to State Bank of India 7067
₹1,000

02 Aug, 2026
10:55 PM
Paid to NATRAJ VEGIS
UPI Transaction ID: 621417930712
Paid by State Bank of India 7067
₹1,175

04 Aug, 2026
01:39 PM
Received from SHREYA DADASO KOLI
UPI Transaction ID: 621614198531
Paid to State Bank of India 7067
₹670

04 Aug, 2026
02:26 PM
Paid to UMAR GOUSPEER JAHAGIRDAR
UPI Transaction ID: 621608260214
Paid by State Bank of India 7067
₹1,100

04 Aug, 2026
02:32 PM
Paid to MC DONALDS
UPI Transaction ID: 658284760149
Paid by State Bank of India 7067
₹236.24

04 Aug, 2026
03:35 PM
Paid to SHRIRAM PETROLEUM
UPI Transaction ID: 621612162874
Paid by State Bank of India 7067
₹200

04 Aug, 2026
04:09 PM
Received from SWAPNIL RAVASO MOKASHI
UPI Transaction ID: 110360988861
Paid to State Bank of India 7067
₹240

04 Aug, 2026
07:24 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110362190948
Paid to State Bank of India 7067
₹366

05 Aug, 2026
11:20 AM
Paid to AKRAM SHAIKH
UPI Transaction ID: 621752158496
Paid by State Bank of India 7067
₹18,355

05 Aug, 2026
01:04 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110368190321
Paid to State Bank of India 7067
₹500

05 Aug, 2026
03:48 PM
Received from Amruta Mote
UPI Transaction ID: 127415973863
Paid to State Bank of India 7067
₹600

05 Aug, 2026
05:19 PM
Paid to ANIL RAJU SHETTY
UPI Transaction ID: 621767883290
Paid by State Bank of India 7067
₹15

06 Aug, 2026
06:27 PM
Received from SUJATA SHITAL MULAY
UPI Transaction ID: 386506186791
Paid to State Bank of India 7067
₹2,550

06 Aug, 2026
08:09 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110379636617
Paid to State Bank of India 7067
₹330

06 Aug, 2026
08:15 PM
Paid to Zomato
UPI Transaction ID: 621835978214
Paid by State Bank of India 7067
₹433.46

06 Aug, 2026
08:15 PM
Received from VEDANT SATISH JADHAV
UPI Transaction ID: 233413662653
Paid to State Bank of India 7067
₹86

06 Aug, 2026
08:15 PM
Received from Aditya Kerimane
UPI Transaction ID: 621891825876
Paid to State Bank of India 7067
₹86

06 Aug, 2026
08:36 PM
Paid to VEDANT SATISH JADHAV
UPI Transaction ID: 621837707006
Paid by State Bank of India 7067
₹86

06 Aug, 2026
08:37 PM
Paid to Manthan Anil Vanamore
UPI Transaction ID: 621837834373
Paid by State Bank of India 7067
₹360

06 Aug, 2026
08:37 PM
Paid to Manthan Anil Vanamore
UPI Transaction ID: 621837846796
Paid by State Bank of India 7067
₹10

06 Aug, 2026
08:38 PM
Received from VEDANT SATISH JADHAV
UPI Transaction ID: 261187175765
Paid to State Bank of India 7067
₹85

06 Aug, 2026
10:42 PM
Received from VEDANT SATISH JADHAV
UPI Transaction ID: 403376068100
Paid to State Bank of India 7067
₹85

06 Aug, 2026
10:42 PM
Received from Aditya Kerimane
UPI Transaction ID: 621867538017
Paid to State Bank of India 7067
₹85

07 Aug, 2026
11:41 PM
Paid to Shruti Birje
UPI Transaction ID: 658589831272
Paid by State Bank of India 7067
₹1,000

07 Aug, 2026
11:42 PM
Received from Shruti Birje
UPI Transaction ID: 621907733820
Paid to State Bank of India 7067
₹1,000

08 Aug, 2026
09:52 AM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110389910985
Paid to State Bank of India 7067
₹300

08 Aug, 2026
11:46 AM
Received from ADARSH VIJAY SINDAGI
UPI Transaction ID: 658602896491
Paid to State Bank of India 7067
₹120

09 Aug, 2026
01:44 PM
Paid to TRENT LIMITED
UPI Transaction ID: 622181990981
Paid by State Bank of India 7067
₹949.05

10 Aug, 2026
11:20 AM
Received from Dilip Mulay
UPI Transaction ID: 658838709965
Paid to State Bank of India 7067
₹1,200

12 Aug, 2026
03:50 PM
Received from Miss TANVI ZAKRDE
UPI Transaction ID: 542194953875
Paid to State Bank of India 7067
₹3,000

12 Aug, 2026
05:36 PM
Paid to ROYAL ENFIELD HARE MADHAV
UPI Transaction ID: 622472017990
Paid by State Bank of India 7067
₹20,000

12 Aug, 2026
07:20 PM
Received from Snehal Mulay
UPI Transaction ID: 659037118133
Paid to State Bank of India 7067
₹3,110

13 Aug, 2026
10:14 AM
Paid to Ganga Furnishing
UPI Transaction ID: 622505989684
Paid by State Bank of India 7067
₹17,492

13 Aug, 2026
10:15 AM
Paid to Kapil Dev
UPI Transaction ID: 622506046196
Paid by State Bank of India 7067
₹11,286

13 Aug, 2026
10:16 AM
Paid to DUDH ANI DUGDHJANYA PADARTHA UTPADAK ASSOCIATION SANGLI
UPI Transaction ID: 622506055953
Paid by State Bank of India 7067
₹7,000

13 Aug, 2026
10:16 AM
Paid to AKRAM SHAIKH
UPI Transaction ID: 622506111289
Paid by State Bank of India 7067
₹20,000

13 Aug, 2026
04:28 PM
Received from VEDANT SATISH JADHAV
UPI Transaction ID: 071465376188
Paid to State Bank of India 7067
₹163

13 Aug, 2026
04:28 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 742118650992
Paid to State Bank of India 7067
₹162

13 Aug, 2026
04:28 PM
Received from Aditya almane
UPI Transaction ID: 659192021237
Paid to State Bank of India 7067
₹165

13 Aug, 2026
04:29 PM
Received from Aditya Kerimane
UPI Transaction ID: 659110960663
Paid to State Bank of India 7067
₹163

14 Aug, 2026
05:03 PM
Received from Rishit Choksi
UPI Transaction ID: 127906555866
Paid to India Post Payment Bank 2938
₹21,000

14 Aug, 2026
11:20 PM
Received from VEDANT SATISH JADHAV
UPI Transaction ID: 190598350689
Paid to State Bank of India 7067
₹90

14 Aug, 2026
11:20 PM
Received from Adarsh Vijay Sindagi
UPI Transaction ID: 004108245923
Paid to State Bank of India 7067
₹100

14 Aug, 2026
11:21 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 342458528538
Paid to State Bank of India 7067
₹180

16 Aug, 2026
01:54 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110450766685
Paid to State Bank of India 7067
₹330

17 Aug, 2026
10:28 PM
Self transfer to State Bank of India 7067
UPI Transaction ID: 659539187251
Paid by India Post Payment Bank 2938
₹1

18 Aug, 2026
09:07 PM
Paid to YouTube
UPI Transaction ID: 800558472306
Paid by State Bank of India 7067
₹179

20 Aug, 2026
05:45 PM
Received from RAJENDRA PATEL
UPI Transaction ID: 128224924225
Paid to India Post Payment Bank 2938
₹12,000

22 Aug, 2026
12:58 PM
Received from MOHD SAJID AHMED
UPI Transaction ID: 090005252325
Paid to India Post Payment Bank 2938
₹5,500

22 Aug, 2026
08:25 PM
Paid to Shafik Mulla
UPI Transaction ID: 623439265266
Paid by State Bank of India 7067
₹10,000

23 Aug, 2026
08:57 AM
Paid to Flipkart
UPI Transaction ID: 660183020237
Paid by State Bank of India 7067
₹830

23 Aug, 2026
02:56 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110502618260
Paid to State Bank of India 7067
₹100

23 Aug, 2026
04:06 PM
Paid to SPOTIFY INDIA PVT LTD
UPI Transaction ID: 103901502808
Paid by State Bank of India 7067
₹69

24 Aug, 2026
11:48 AM
Received from Abhishak Chakraborty
UPI Transaction ID: 623617222678
Paid to State Bank of India 7067
₹20,500

24 Aug, 2026
11:50 AM
Paid to 2787
UPI Transaction ID: 623610326935
Paid by India Post Payment Bank 2938
₹25,000

24 Aug, 2026
11:50 AM
Paid to 2787
UPI Transaction ID: 623692026232
Paid by India Post Payment Bank 2938
₹25,000

24 Aug, 2026
11:51 AM
Self transfer to India Post Payment Bank 2938
UPI Transaction ID: 623657244814
Paid by State Bank of India 7067
₹20,500

24 Aug, 2026
02:24 PM
Paid to Kishor Subhash Jadhav
UPI Transaction ID: 623627202154
Paid by State Bank of India 7067
₹150

24 Aug, 2026
02:25 PM
Paid to Kiran Trading Company
UPI Transaction ID: 623627262783
Paid by State Bank of India 7067
₹60

25 Aug, 2026
09:44 AM
Received from Soham Mulay
UPI Transaction ID: 128464281626
Paid to State Bank of India 7067
₹2,000

25 Aug, 2026
12:02 PM
Paid to Sadashiv Vagyapapa Adisare
UPI Transaction ID: 623777947135
Paid by State Bank of India 7067
₹10

25 Aug, 2026
12:08 PM
Paid to Mangave petrolinks
UPI Transaction ID: 623778348593
Paid by State Bank of India 7067
₹10

25 Aug, 2026
02:05 PM
Paid to GANGA XEROX
UPI Transaction ID: 623785910503
Paid by State Bank of India 7067
₹3

25 Aug, 2026
02:11 PM
Paid to GANGA XEROX
UPI Transaction ID: 623786251970
Paid by State Bank of India 7067
₹6

25 Aug, 2026
04:19 PM
Paid to Mane Tailors
UPI Transaction ID: 623793516623
Paid by State Bank of India 7067
₹30

25 Aug, 2026
04:25 PM
Paid to Chay 24M
UPI Transaction ID: 623793922285
Paid by State Bank of India 7067
₹12

25 Aug, 2026
07:30 PM
Paid to REDBUS
UPI Transaction ID: 623708177890
Paid by State Bank of India 7067
₹1,258.95

25 Aug, 2026
08:37 PM
Paid to Jaikishan Patel
UPI Transaction ID: 660380575024
Paid by State Bank of India 7067
₹49

26 Aug, 2026
08:59 AM
Paid to SACHIN SITARAM KATKAR
UPI Transaction ID: 623826525790
Paid by State Bank of India 7067
₹20

26 Aug, 2026
09:00 AM
Paid to Indian Railways
UPI Transaction ID: 623826568203
Paid by State Bank of India 7067
₹10

26 Aug, 2026
11:02 AM
Received from ANIKETDILIPMULAY
UPI Transaction ID: 110224061107
Paid to State Bank of India 7067
₹2,000

26 Aug, 2026
11:38 AM
Received from PRANAV DEELIP MULAY
UPI Transaction ID: 340220612986
Paid to State Bank of India 7067
₹2,000

26 Aug, 2026
01:15 PM
Received from kadam siddharth
UPI Transaction ID: 623871202835
Paid to State Bank of India 7067
₹1,000

26 Aug, 2026
05:06 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110526231981
Paid to State Bank of India 7067
₹200

26 Aug, 2026
08:23 PM
Paid to Jaish Momin
UPI Transaction ID: 623868756610
Paid by State Bank of India 7067
₹530

26 Aug, 2026
09:14 PM
Received from VEDANT SATISH JADHAV
UPI Transaction ID: 630267987884
Paid to State Bank of India 7067
₹230

27 Aug, 2026
09:24 AM
Paid to M/S.MORE MART
UPI Transaction ID: 623977874355
Paid by State Bank of India 7067
₹30

27 Aug, 2026
11:04 AM
Received from Abhishak Chakraborty
UPI Transaction ID: 623989325300
Paid to State Bank of India 7067
₹12,000

27 Aug, 2026
11:17 AM
Paid to M/S.AVINASH CARGO PRIVATE LIMITED
UPI Transaction ID: 623990102010
Paid by State Bank of India 7067
₹335

27 Aug, 2026
04:37 PM
Received from Dilip Mulay
UPI Transaction ID: 623980082467
Paid to State Bank of India 7067
₹355

27 Aug, 2026
04:41 PM
Received from Manan Panchal
UPI Transaction ID: 623927794423
Paid to India Post Payment Bank 2938
₹14,000

27 Aug, 2026
07:19 PM
Paid to OMKAR ANAND GADADE
UPI Transaction ID: 623923011396
Paid by State Bank of India 7067
₹1,900

28 Aug, 2026
04:55 PM
Received from Aditya Kerimane
UPI Transaction ID: 660670471167
Paid to State Bank of India 7067
₹230

29 Aug, 2026
12:53 PM
Paid to IRUVE BAKE AND BREW
UPI Transaction ID: 624116597964
Paid by State Bank of India 7067
₹20

29 Aug, 2026
05:40 PM
Paid to Jio Prepaid
UPI Transaction ID: 624133416248
Paid by State Bank of India 7067
₹19

29 Aug, 2026
05:42 PM
Paid to JIO
UPI Transaction ID: 624133576673
Paid by State Bank of India 7067
₹349

29 Aug, 2026
05:53 PM
Paid to Miss SONAL SHITAL MULAY
UPI Transaction ID: 624134346620
Paid by State Bank of India 7067
₹2,000

29 Aug, 2026
05:56 PM
Paid to BLINKIT
UPI Transaction ID: 624134556420
Paid by State Bank of India 7067
₹1,511

29 Aug, 2026
08:04 PM
Paid to TORQ 03 SPORTS AND
UPI Transaction ID: 624145295871
Paid by State Bank of India 7067
₹100

29 Aug, 2026
10:18 PM
Paid to MAHESH
UPI Transaction ID: 660701473335
Paid by State Bank of India 7067
₹30

30 Aug, 2026
07:16 PM
Received from NIHAL JAHID SHAIKH
UPI Transaction ID: 110556408223
Paid to State Bank of India 7067
₹300

31 Aug, 2026
09:36 AM
Received from Piyusha Mulay
UPI Transaction ID: 624318579328
Paid to State Bank of India 7067
₹600

31 Aug, 2026
06:42 PM
Received from MAKARAND ANAND POTDAR
UPI Transaction ID: 313643449983
Paid to State Bank of India 7067
₹2,000
"""

lines = [l.strip() for l in statement_text.strip().split('\n') if l.strip()]

date_re = re.compile(r"^(\d{1,2}\s+[A-Za-z]{3},\s+\d{4})$")
time_re = re.compile(r"^(\d{1,2}:\d{2}\s+(?:AM|PM|am|pm))$")
amount_re = re.compile(r"^₹\s*([0-9,]+(?:\.[0-9]+)?)$")

transactions = []
i = 0
while i < len(lines):
    line = lines[i]
    if date_re.match(line):
        date_part = line
        time_part = ""
        action_line = ""
        upi_id = ""
        bank_line = ""
        amount = 0.0

        j = i + 1
        while j < len(lines) and j < i + 10:
            nxt = lines[j]
            if date_re.match(nxt):
                break
            if time_re.match(nxt):
                time_part = nxt
            elif nxt.startswith("Received from ") or nxt.startswith("Self transfer"):
                action_line = nxt
            elif not action_line and nxt.startswith("Paid to "):
                action_line = nxt
            elif "UPI Transaction ID:" in nxt:
                upi_id = nxt.split(":")[-1].strip()
            elif nxt.startswith("Paid by ") or "State Bank" in nxt or "India Post" in nxt:
                bank_line = re.sub(r"^(Paid by|Paid to)\s+", "", nxt).strip()
            else:
                m = amount_re.match(nxt)
                if m:
                    amount = float(m.group(1).replace(",", ""))
                    j += 1
                    break
            j += 1

        if action_line and amount > 0:
            tx_type = "EXPENSE"
            if action_line.startswith("Self transfer"):
                tx_type = "SELF_TRANSFER"
            elif action_line.startswith("Received from"):
                tx_type = "INCOME"

            transactions.append({
                "date": f"{date_part} {time_part}",
                "action": action_line,
                "amount": amount,
                "type": tx_type,
                "upi_id": upi_id,
                "bank": bank_line or "State Bank of India 7067"
            })
            i = j - 1
    i += 1

print(f"Total parsed transactions: {len(transactions)}")

expenses = [t for t in transactions if t["type"] == "EXPENSE"]
incomes = [t for t in transactions if t["type"] == "INCOME"]
transfers = [t for t in transactions if t["type"] == "SELF_TRANSFER"]

total_sent = sum(t["amount"] for t in expenses) + sum(t["amount"] for t in transfers)
total_recv = sum(t["amount"] for t in incomes) + sum(t["amount"] for t in transfers)

print(f"Total Outflow (Expenses + Transfers): ₹{total_sent:,.2f}")
print(f"Total Inflow (Received + Transfers): ₹{total_recv:,.2f}")
print(f"Net Real Expenses (excluding self-transfers): ₹{sum(t['amount'] for t in expenses):,.2f}")
print(f"Self-transfers found: {len(transfers)} items totaling ₹{sum(t['amount'] for t in transfers):,.2f}")

# Save parsed dataset to json for verification and companion app
with open("/Users/sahilashokmulay/Desktop/projects/expense-manager-android/sample_august_statement.json", "w") as f:
    json.dump(transactions, f, indent=2)

print("Saved sample_august_statement.json successfully!")
