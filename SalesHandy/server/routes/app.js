const express = require('express'); // import express module
const database = require('../database'); // import custom database module

const router = express.Router();

// ----------------------- daily report endpoint -------------------
router.get('/daily', async (req, res) => {
    try {
        let results = await database.getVaccinatedPersonReportByDaily();
        res.json(results);
    }
    catch(exception) {
        res.sendStatus(500);
    }
});

// ----------------------- weekly report endpoint -------------------
router.get('/weekly', async (req, res) => {
    try {
        let results = await database.getVaccinatedPersonReportByWeekly();
        res.json(results);
    }
    catch(exception) {
        res.send(exception);
    }
});

// ----------------------- monthly report endpoint -------------------
router.get('/monthly', async (req, res) => {
    try {
        let results = await database.getVaccinatedPersonReportByMonthly();
        res.json(results);
    }
    catch(exception) {
        res.send(exception);
    }
});

// ----------------------- yearly report endpoint -------------------
router.get('/yearly', async (req, res) => {
    try {
        let results = await database.getVaccinatedPersonReportByYearly();
        res.json(results);
    }
    catch(exception) {
        res.send(exception);
    }
});

// ----------------------- location based report endpoint -------------------
router.get('/location/:locationName', async (req, res) => {
    try {
        let results = await database.getVaccinatedPersonReportByLocation(req.params.locationName);
        res.json(results);
    }
    catch(exception) {
        res.send(exception);
    }
});

// ----------------------- date vise report endpoint -------------------
router.get('/dateFilter', async (req, res) => {
    try {
        let results = await database.getVaccinatedPersonReportByDateRange(req, res);
        res.json(results);
    }
    catch(exception) {
        res.send('startDate and endDate range must be proper...');
    }
})

module.exports = router;