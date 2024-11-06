'use strict';

module.exports = {
    uploadRandomizedUser,
    processRegisterReply,
    prepareGetUser,
    prepareUpdateUser,
    prepareDeleteUser,
    prepareSearchPattern,
    saveShortId,
    extractBlobDetails
};

const fs = require('fs');

var registeredUsers = [];
var users = [];

var statsPrefix = [
    ["/rest/media/", "GET"],
    ["/rest/media", "POST"]
];

global.myProcessEndpoint = function (str, method) {
    for (let i = 0; i < statsPrefix.length; i++) {
        if (str.startsWith(statsPrefix[i][0]) && method === statsPrefix[i][1]) {
            return method + ":" + statsPrefix[i][0];
        }
    }
    return method + ":" + str;
};

function randomUsername(charLimit) {
    const letters = 'abcdefghijklmnopqrstuvwxyz';
    let username = '';
    for (let i = 0; i < charLimit; i++) {
        username += letters[Math.floor(Math.random() * letters.length)];
    }
    return username;
}

function randomPassword(passLen) {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*';
    let password = '';
    for (let i = 0; i < passLen; i++) {
        password += chars[Math.floor(Math.random() * chars.length)];
    }
    return password;
}

function processRegisterReply(requestParams, response, context, ee, next) {
    if (response.body && response.body.length > 0) {
        registeredUsers.push(response.body);
    }
    return next();
}

function uploadRandomizedUser(requestParams, context, ee, next) {
    let username = randomUsername(10);
    let password = randomPassword(8);
    requestParams.body = JSON.stringify({
        userId: username,
        pwd: password,
        email: `${username}@example.com`,
        displayName: username
    });
    users.push({ userId: username, pwd: password });
    return next();
}

let usersSearched = 0;

function prepareGetUser(requestParams, context, ee, next) {
    const user = users[usersSearched++];
    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.url = requestParams.url.replace('{{ pwd }}', user.pwd);
    return next();
}

// Prepare request to update a specific user
function prepareUpdateUser(requestParams, context, ee, next) {
    const user = users.find(u => u.userId === context.vars.userId);

    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.qs = { pwd: user.pwd };
    requestParams.json = { ...user, displayName: `Updated_${user.displayName}` };

    return next();
}

// Prepare request to delete a specific user
function prepareDeleteUser(requestParams, context, ee, next) {
    const user = users.find(u => u.userId === context.vars.userId);

    requestParams.url = requestParams.url.replace('{{ userId }}', user.userId);
    requestParams.qs = { pwd: user.pwd };

    return next();
}

// Prepare request to search for users with a pattern
function prepareSearchPattern(requestParams, context, ee, next) {
    context.vars.queryPattern = "testPattern"; // Customize the pattern as needed
    requestParams.qs = { query: context.vars.queryPattern };

    return next();
}

function saveShortId(requestParams, response, context, ee, next) {
    if (response.statusCode === 200) {
        const responseBody = JSON.parse(response.body);
        context.vars.shortId = responseBody.shortId;
        console.log(`Short ID saved: ${response.body}`);
    } else {
        console.error("Failed to create short. Response:", response.body);
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
