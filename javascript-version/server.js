import express from "express";
import pg from "pg";

const { Pool } = pg;
const app = express();
const PORT = process.env.PORT || 5000;

// Parse form submissions
app.use(express.urlencoded({ extended: true }));

function jdbcToPgConfig(jdbcUrl) {
    if (!jdbcUrl) {
        throw new Error("JDBC_DATABASE_URL is not set.");
    }

    if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
        throw new Error('JDBC_DATABASE_URL must start with "jdbc:postgresql://".');
    }

    const withoutJdbc = jdbcUrl.replace("jdbc:", "");
    const parsed = new URL(withoutJdbc);

    return {
        host: parsed.hostname,
        port: Number(parsed.port || 5432),
        database: parsed.pathname.replace(/^\//, ""),
        user: parsed.searchParams.get("user"),
        password: parsed.searchParams.get("password"),
        ssl: process.env.NODE_ENV === "production" ? { rejectUnauthorized: false } : false,
    };
}

const pool = new Pool(jdbcToPgConfig(process.env.JDBC_DATABASE_URL));

async function ensureTableExists() {
    await pool.query(`
    CREATE TABLE IF NOT EXISTS table_timestamp_and_random_string (
      tick timestamp,
      random_string varchar(50)
    )
  `);
}

function getRandomString() {
    const chars =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    let result = "";

    for (let i = 0; i < 8; i += 1) {
        const randomIndex = Math.floor(Math.random() * chars.length);
        result += chars[randomIndex];
    }

    return result;
}

app.get("/", (req, res) => {
    res.send(`
    <h1>Hello World!</h1>
    <p><a href="/database">View database entries</a></p>
    <p><a href="/dbinput">Add a string to the database</a></p>
  `);
});

app.get("/database", async (req, res) => {
    console.log("Caleb Corolewski");
    console.log("Caleb accessed /database");

    try {
        await ensureTableExists();

        const result = await pool.query(`
      SELECT tick, random_string
      FROM table_timestamp_and_random_string
      ORDER BY tick DESC
    `);

        let html = `
      <h1>Database Entries</h1>
      <p><a href="/dbinput">Add another string</a></p>
      <ul>
    `;

        for (const row of result.rows) {
            html += `<li>${row.tick} | ${row.random_string ?? ""}</li>`;
        }

        html += "</ul>";

        res.send(html);
    } catch (error) {
        res.status(500).send(`Error: ${error.message}`);
    }
});

app.get("/dbinput", (req, res) => {
    res.send(`
    <h1>Enter a String</h1>
    <form method="post" action="/dbinput">
      <label for="userInput">String:</label>
      <input type="text" id="userInput" name="userInput" maxlength="50" required>
      <button type="submit">Submit</button>
    </form>
    <p><a href="/database">View database entries</a></p>
  `);
});

app.post("/dbinput", async (req, res) => {
    const { userInput } = req.body;

    try {
        await ensureTableExists();

        await pool.query(
            "INSERT INTO table_timestamp_and_random_string (tick, random_string) VALUES (NOW(), $1)",
            [userInput]
        );

        console.log(`Caleb submitted to /dbinput: ${userInput}`);

        res.send(`
      <h1>Input Saved</h1>
      <p>Your string was added to the database.</p>
      <p><a href="/dbinput">Add another string</a></p>
      <p><a href="/database">View database entries</a></p>
    `);
    } catch (error) {
        res.status(500).send(`Error: ${error.message}`);
    }
});

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});