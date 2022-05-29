const express = require('express'); // import express module
const userPostAPIRouter = require('./routes/index.js'); // import custom routes/index module
const userReportAPIRouter = require('./routes/app.js'); // import custom routes/app module

const app = express();

app.use(express.json());

app.use('/userPostAPI/vaccinated', userPostAPIRouter);
app.use('/userReportAPI/vaccinated', userReportAPIRouter);

app.listen(process.env.PORT || `3000`, () => {
    console.log(`Server is running on port : ${process.env.PORT || `3000`}`);
})