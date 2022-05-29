const express = require('express'); // import express module
const database = require('../database'); // import custom database module

const router = express.Router();

// -------------------- add person endpoint ------------------------
router.post('/addPerson', (req, res) => {
    try {
        let results = database.insertVaccinatedPersonDetail(req, res);
        res.sendStatus(200);      
    }
    catch(exception) {
        res.sendStatus(500);
    }
});

module.exports = router;