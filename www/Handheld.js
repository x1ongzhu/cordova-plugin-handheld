var exec = require("cordova/exec");

exports.scanCode = function(success, error) {
    exec(success, error, "Handheld", "scanCode", []);
};
exports.stopScan = function(success, error) {
    exec(success, error, "Handheld", "stopScan", []);
};
exports.readTag = function(success, error, options) {
    exec(success, error, "Handheld", "readTag", [options]);
};
exports.stopRead = function(success, error) {
    exec(success, error, "Handheld", "stopRead", []);
};
exports.getPicture = function(success, error, cameraOptions) {
    exec(
        function() {
            navigator.camera.getPicture(
                function(res) {
                    exec(function() {}, function() {}, "Handheld", "resume", []);
                    try {
                        success(res);
                    } catch (e) {}
                },
                function(err1) {
                    exec(function() {}, function() {}, "Handheld", "resume", []);
                    try {
                        error(err1);
                    } catch (e) {}
                },
                cameraOptions
            );
        },
        function(err) {
            try {
                error(err);
            } catch (e) {}
        },
        "Handheld",
        "pause",
        []
    );
};
