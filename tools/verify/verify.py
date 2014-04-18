import hashlib
import json

dataOfResult = json.load(open("data.json"))
dataString = ""
for dataElm in dataOfResult:
	dataString += dataElm["email"] + dataElm["str"] + str(dataElm["time"])
sha256 = hashlib.sha256()
sha256.update(dataString)

resultInHex = sha256.hexdigest()

resultInInt = int(resultInHex,16)

winner = resultInInt % len(dataOfResult)

print "Winner index is : " + str(winner)

print "Winner's email is : " + dataOfResult[winner]["email"]