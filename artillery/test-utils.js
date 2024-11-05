'use strict';

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
  uploadRandomizedUser,
  processRegisterReply,
  prepareGetUser
}


const fs = require('fs') // Needed for access to blobs.

var registeredUsers = []
var images = []
var users = []

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [ ["/rest/media/","GET"],
			["/rest/media","POST"]
	]

// Function used to compress statistics
global.myProcessEndpoint = function( str, method) {
	var i = 0;
	for( i = 0; i < statsPrefix.length; i++) {
		if( str.startsWith( statsPrefix[i][0]) && method == statsPrefix[i][1])
			return method + ":" + statsPrefix[i][0];
	}
	return method + ":" + str;
}

// Returns a random username constructed from lowercase letters.
function randomUsername(char_limit){
    const letters = 'abcdefghijklmnopqrstuvwxyz';
    let username = '';
    let num_chars = Math.floor(Math.random() * char_limit);
    for (let i = 0; i < char_limit; i++) {
        username += letters[Math.floor(Math.random() * letters.length)];
    }
    return username;
}


// Returns a random password, drawn from printable ASCII characters
function randomPassword(pass_len){
    const skip_value = 33;
    const lim_values = 94;
    
    let password = '';
    let num_chars = Math.floor(Math.random() * pass_len);
    for (let i = 0; i < pass_len; i++) {
        let chosen_char =  Math.floor(Math.random() * lim_values) + skip_value;
        if (chosen_char == "'" || chosen_char == '"')
            i -= 1;
        else
            password += chosen_char
    }
    return password;
}

/**
 * Process reply of the user registration.
 */
function processRegisterReply(requestParams, response, context, ee, next) {
    if( typeof response.body !== 'undefined' && response.body.length > 0) {
        registeredUsers.push(response.body);
    }
    return next();
} 

/**
 * Register a random user.
 */

function uploadRandomizedUser(requestParams, context, ee, next) {
    let username = randomUsername(15);
    let pword = randomPassword(5);
    let email = username + "@campus.fct.unl.pt";
    let displayName = username;
    
    const user = {
        userId: username,
        pwd: pword,
        email: email,
        displayName: displayName
    };
    requestParams.body = JSON.stringify(user);
    users.push(user);
    return next();
}

let users_searched = 0;

// Prepare user retrieval by setting the user ID in the URL
function prepareGetUser(requestParams, context, ee, next) {
    const user = users[users_searched++];
    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.url = requestParams.url.replace('{{ pwd }}', user.pwd);
    //console.log(`Attempting to retrieve user with ID: ${user.userId}`);
    return next();
}

function generateUser(){
    let username = randomUsername(15);
    let pword = randomPassword(5);
    let email = username + "@campus.fct.unl.pt";
    let displayName = username;
    
    const user = {
        userId: username,
        pwd: pword,
        email: email,
        displayName: displayName
    };
    return user;
}

// Function to append a user to the CSV file
function saveUserToCSV(user) {
    const filePath = 'data/users.csv';
    const csvLine = `${user.userId},${user.pwd},${user.email},"${user.displayName}"\n`;

    // Check if file exists; if not, add the header
    if (!fs.existsSync(filePath)) {
        fs.writeFileSync(filePath, "userid,pwd,email,displayName\n", { flag: 'w' });
    }

    // Append user data to the file
    fs.appendFileSync(filePath, csvLine, { flag: 'a' });
}

/*for( var i = 0; i < 1000; i++) {
    var user = generateUser();
    saveUserToCSV(user);
}*/