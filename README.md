# wb-ticketprototype
This is a proof-of-concept prototype of a ticketing scheme for informal transport being developed by the World Bank.

The BlueToothDEMO folder contains a modified version of the app which comes with an EDAL thermal receipt printer. The Obsqr folder contains a modified version of the Obsqr app, which is available on GitHub at https://github.com/trikita/obsqr. The apps work together.

Upon scanning a valid QR Code, the app in the Obsqr folder will send an HTTP request to an IFTTT endpoint, which adds a line into a Google Spreadsheet.
