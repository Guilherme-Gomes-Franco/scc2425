'use strict';

module.exports = {
    uploadRandomizedUser,
    processRegisterReply,
    prepareGetUser,
    prepareUpdateUser,
    prepareDeleteUser,
    processUpdateReply,
    prepareSearchPattern,
    saveShort,
    prepareCreateShort,
    prepareGetShort,
    prepareAddLike,
    processLike,
    extractBlobDetails
};

const fs = require('fs');

var registeredUsers = {};
var registeredShorts = {};

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [["/rest/media/", "GET"],
["/rest/media", "POST"]
["/rest/media", "DELETE"],
["/rest/media", "PUT"]
]

// Function used to compress statistics
global.myProcessEndpoint = function (str, method) {
    var i = 0;
    for (i = 0; i < statsPrefix.length; i++) {
        if (str.startsWith(statsPrefix[i][0]) && method == statsPrefix[i][1])
            return method + ":" + statsPrefix[i][0];
    }
    return method + ":" + str;
}


function randomUsername(charLimit) {
    const letters = 'abcdefghijklmnopqrstuvwxyz';
    let username = '';
    for (let i = 0; i < charLimit; i++) {
        username += letters[Math.floor(Math.random() * letters.length)];
    }
    return username;
}

// Returns a random password, drawn from printable ASCII characters
function randomPassword(pass_len) {
    const skip_value = 33;
    const lim_values = 94;

    let password = '';
    for (let i = 0; i < pass_len; i++) {
        let chosen_char = Math.floor(Math.random() * lim_values) + skip_value;
        if (chosen_char == "'" || chosen_char == '"')
            i -= 1;
        else
            password += chosen_char
    }
    return password;
}

function processRegisterReply(requestParams, response, context, ee, next) {
    if (typeof response.body !== 'undefined' && response.body.length > 0) {
        registeredUsers[response.body.userId] = response.body;
    }
    return next();
}

function processUpdateReply(requestParams, response, context, ee, next) {
    if (typeof response.body !== 'undefined' && response.body.length > 0) {
        registeredUsers[response.body.userId] = response.body;
    }
    return next();
}

function uploadRandomizedUser(requestParams, context, ee, next) {
    let username = randomUsername(10);
    let password = randomPassword(8);
    requestParams.body = JSON.stringify({
        userId: username,
        pwd: password,
        email: username + "@example.com",
        displayName: username
    });
    return next();
}

function prepareGetUser(requestParams, context, ee, next) {
    const user = users[getRandomUserIndex()];
    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.url = requestParams.url.replace('{{ pwd }}', user.pwd);
    return next();
}

// Prepare request to update a specific user
function prepareUpdateUser(requestParams, context, ee, next) {
    const user = registeredUsers[getRandomUserIndex()];

    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.url = requestParams.url.replace('{{ pwd }}', user.pwd);
    requestParams.body = JSON.stringify({
        userId: user.userId,
        pwd: user.pwd,
        email: user.email,
        displayName: "updated_" + user.displayName
    });

    return next();
}

// Prepare request to delete a specific user
function prepareDeleteUser(requestParams, context, ee, next) {
    //const user = users.find(u => u.userId === context.vars.userId);
    const user = registeredUsers[getRandomUserIndex()];

    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.url = requestParams.url.replace('{{ pwd }}', user.pwd);

    return next();
}

// Prepare request to search for users with a pattern
function prepareSearchPattern(requestParams, context, ee, next) {
    // context.vars.queryPattern = "testPattern"; // Customize the pattern as needed
    requestParams.url = requestParams.url.replace('{{ queryPattern }}', "xy");

    return next();
}

function prepareCreateShort(requestParams, context, ee, next) {
    const user = registeredUsers[getRandomUserIndex()];
    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    return next();
}

function saveShort(requestParams, response, context, ee, next) {
    if (typeof response.body !== 'undefined' && response.body.length > 0) {
        registeredShorts[response.body.shortId] = response.body;
    }
    requestParams.log = requestParams.log.replace('{{ blobUrl }}', response.body.blobUrl);
    return next();
}

function prepareGetShort(requestParams, context, ee, next) {
    const short = registeredShorts[getRandomShortIndex()];
    requestParams.url = requestParams.url.replace('{{ shortId }}', short.shortId);
    return next();
}

function prepareAddLike(requestParams, context, ee, next) {
    const short = registeredShorts[getRandomShortIndex()];
    requestParams.url = requestParams.url.replace('{{ shortId }}', short.shortId);
    requestParams.url = requestParams.url.replace('{{ userId }}', short.ownerId);
    requestParams.url = requestParams.url.replace('{{ pwd }}', user.pwd);
    return next();
}

function processLike(requestParams, response, context, ee, next) {
    if (typeof response.body !== 'undefined' && response.body.length > 0) {
        registeredUsers.push(response.body);
    }
    return next();
}

// Extract blobId and token from the blobUrl
function extractBlobDetails(requestParams, response, context, ee, next) {
    if (context.vars.blobUrl) {
        const url = new URL(context.vars.blobUrl);
        context.vars.blobId = url.pathname.split('/').pop();
        context.vars.token = url.searchParams.get("token");
        console.log(`Extracted blobId: ${context.vars.blobId}, token: ${context.vars.token}`);
    }
    return next();
}

function getRandomUserKey() {
    return Math.floor(Math.random() * registeredUsers.length);
}

function getRandomShortKey() {
    return Math.floor(Math.random() * registeredShorts.length);
}