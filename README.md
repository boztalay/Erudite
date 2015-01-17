# Erudite
Mine and @benzweig's MHacks V hack, a Google Glass app that listens to your conversation and lets you pull the Wikipedia page for certain words, so you can sound smarter.

Usage
-----

"Ok, Glass. Make me smarter", or choose "Make me smarter" from the menu. Then, just talk. It'll listen for words that aren't stop words (and, but, etc), and display the most recent 20 in a list you can swipe through. Tap on a word and it'll pull down the first paragraph of that word's Wikipedia page. Tap again to have it read to you.

NOTE: This app eats batteries for breakfast, lunch, and dinner. It's spamming the SpeechRecognizer API in a way it shouldn't be used, which forces a ton of network activity. It also keeps it on. Your Glass will get hot.
