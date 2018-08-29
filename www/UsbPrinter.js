var exec = require('cordova/exec');

module.exports.add = function(arg0, success, error){
    exec(success, error, 'UsbPrinter', 'add',[arg0]);
}

module.exports.substract = function(arg0, success, error){
    exec(success, error, 'UsbPrinter', 'substract',[arg0]);
}