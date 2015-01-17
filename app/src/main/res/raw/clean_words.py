fileLines = open("stop_words_raw.txt", "r").readlines()

words = []
for line in fileLines:
    line = line.strip()
    if len(line) <= 0:
        continue
    wordsInLine = line.split("\t")
    for word in wordsInLine:
        word = word.strip()
        if len(word) >= 0:
            words.append(word)

nonDupWords = []
for word in words:
    isDup = False
    for otherWord in nonDupWords:
        if word == otherWord:
            isDup = True
            break
    if not isDup:
        nonDupWords.append(word)

outputFile = open("stop_words_clean.txt", "w")
for word in nonDupWords:
    outputFile.write(word + "\n")
outputFile.close()
