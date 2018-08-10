# wb-ticketprototype
This is a proof-of-concept prototype of a ticketing scheme for informal transport being developed by the World Bank.

The BlueToothDEMO folder contains a modified version of the app which comes with an EDAL thermal receipt printer. The Obsqr folder contains a modified version of the Obsqr app, which is available on GitHub at https://github.com/trikita/obsqr. The apps work together.

Upon scanning a valid QR Code, the app in the Obsqr folder will send an HTTP request to an IFTTT endpoint, which adds a line into a Google Spreadsheet.

# Printer

To print the QR code, press the button "Generate QR Code Print." The rest of the buttons act as they do in the app provided with the EDAL printer.

The generated QR code is a random string multiplied by a prime (for testing, it is currently 78787) that is intended to simulate a scheme in which the printer and scanner can agree on how to generate and verify offline.

# Scanner

The Obsqr code is modified such that upon scanning, it sends a request to an IFTTT endpoint. This request contains a JSON payload, which contains the last known latitude and longitude.

The IFTTT module then sends the location update to the following Google Spreadsheet:

https://docs.google.com/spreadsheets/d/16nxyannmWJ3soRegnM1Ag1VqYFygfCSZa4iIOs4P0ek/edit?usp=sharing 
