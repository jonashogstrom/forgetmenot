![CI](https://github.com/jonashogstrom/forgetmenot/workflows/CI/badge.svg?branch=main)

# forgetmenot
Android app for warning when you forget the phone in the car

The idea is to detect when the charging-status changes from charging to not charging, and the phone is at rest (limited values in the accelerometer). 

Currently the app will by default only detect when losing wireless charging. Non-persistent config allows testing with AC or USB chanrging too.

Current limitations: 
* no guarantee that the app continues to run in the background and isn't killed by the system/os
* not tested how it handles screen lock
* pretty ugly with debug output


