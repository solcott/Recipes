package com.scottolcott.recipe.worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver
