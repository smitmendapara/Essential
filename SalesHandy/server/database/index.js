const mysql = require('mysql'); // import mysql module

// ------------------- database configuration ----------------------------
const pool = mysql.createPool({
    connectionLimit: 10,
    password: 'root',
    user: 'root',
    database: 'vaccination',
    host: 'localhost',
    port: '3306'
}); 

let vaccinatedPerson = {};

// ----------------------------------- generated daily report ----------------------------------------
vaccinatedPerson.getVaccinatedPersonReportByDaily = () => {
    let currentDate = new Date().toISOString().slice(0, 10);

    return new Promise((resolve, reject) => { 
        pool.query(`SELECT * FROM vaccinatedPerson WHERE vaccine_date = ?`, currentDate, (error, results) => {
            if(error) {
                return reject(error);
            }
            return resolve(results);
        })
    })
}

// ----------------------------------- generated weekly report ----------------------------------------
vaccinatedPerson.getVaccinatedPersonReportByWeekly = () => {
    let date = new Date();
    let currentDate = date.toISOString().slice(0, 10);
    date.setDate(date.getDate() - 7);
    let previousDate = date.toISOString().slice(0, 10);

    return new Promise((resolve, reject) => {
        pool.query(`SELECT * FROM vaccinatedPerson WHERE vaccine_date between '${previousDate}' and '${currentDate}'`, (error, results) => {
            if(error){
                return reject(error);
            }
            return resolve(results);
        })
    })
}

// ----------------------------------- generated monthly report ----------------------------------------
vaccinatedPerson.getVaccinatedPersonReportByMonthly = () => {
    var date = new Date();
    let currentDate = date.toISOString().slice(0, 10);
    date.setMonth(date.getMonth() - 1);
    let previousDate = date.toISOString().slice(0, 10);

    return new Promise((resolve, reject) => {
        pool.query(`SELECT * FROM vaccinatedPerson WHERE vaccine_date between '${previousDate}' and '${currentDate}'`, (error, results) => {
            if(error){
                return reject(error);
            }
            return resolve(results);
        })
    })
}

// ----------------------------------- generated yearly report ----------------------------------------
vaccinatedPerson.getVaccinatedPersonReportByYearly = () => {
    var date = new Date();
    let currentDate = date.toISOString().slice(0, 10);
    date.setFullYear(date.getFullYear() - 1);
    let previousDate = date.toISOString().slice(0, 10);

    return new Promise((resolve, reject) => {
        pool.query(`SELECT * FROM vaccinatedPerson WHERE vaccine_date between '${previousDate}' and '${currentDate}'`, (error, results) => {
            if(error){
                return reject(error);
            }
            return resolve(results);
        })
    })
}

// ----------------------------------- generated location based report ----------------------------------------
vaccinatedPerson.getVaccinatedPersonReportByLocation = (locationName) => {
    return new Promise((resolve, reject) => {
        pool.query(`SELECT * FROM vaccinatedPerson WHERE location = ?`, locationName, (error, results) => {
            if(error){
                return reject(error);
            }
            return resolve(results);
        })
    }) 
}

// ----------------------------------- generated date-range vise report ----------------------------------------
vaccinatedPerson.getVaccinatedPersonReportByDateRange = (req, res) => {

    if(Date.parse(req.query.startDate) > Date.parse(req.query.endDate)) {
        return reject();
    }   

    return new Promise((resolve, reject) => {
        pool.query(`SELECT * FROM vaccinatedPerson WHERE vaccine_date between '${req.query.startDate}' and '${req.query.endDate}';`, (error, results) => {
            if(error){
                return reject(error);
            }
            return resolve(results);
        })
    })
}

// ----------------------------------- insert vaccinated person details ----------------------------------------
vaccinatedPerson.insertVaccinatedPersonDetail = (req, res) => {
    const values = [ req.body.aadharCardNumber, req.body.ip_address, req.body.location, req.body.vaccine_date ];
 
    return new Promise((resolve, reject) => {
        pool.query(`INSERT INTO vaccinatedPerson (aadharCardNumber, ip_address, location, vaccine_date) VALUES(?)`, [values], (error, results) => {
            if(error){
                return reject(error);
            }
            return resolve(results);
        })
    })
}

module.exports = vaccinatedPerson;