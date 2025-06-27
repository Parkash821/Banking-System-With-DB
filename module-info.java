// --- 1. module-info.java ---

module BankingSystem {
    requires java.desktop;
    requires java.sql;     
   
   
    exports model;
    exports util;
    exports db;
    exports service;
    exports gui; // Makes GUI classes accessible to Main.java (which is in a different package/module).
}