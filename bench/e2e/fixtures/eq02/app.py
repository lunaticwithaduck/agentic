from flask import Flask, request, session, redirect
import sqlite3

app = Flask(__name__)
app.secret_key = "dev_secret_key_123"

def get_db():
    return sqlite3.connect('users.db')

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    conn = get_db()
    query = f"SELECT * FROM users WHERE username='{username}' AND password='{password}'"
    user = conn.execute(query).fetchone()
    if user:
        session['user'] = username
        session['role'] = user[3]
        return redirect('/dashboard')
    return 'Login failed', 401

@app.route('/admin')
def admin_panel():
    if session.get('role') == 'admin':
        return f"Welcome admin {session['user']}! User count: {get_db().execute('SELECT count(*) FROM users').fetchone()[0]}"
    return 'Forbidden', 403

@app.route('/user/<user_id>')
def get_user(user_id):
    conn = get_db()
    result = conn.execute(f'SELECT name, email FROM users WHERE id = {user_id}').fetchone()
    return f'<h1>{result[0]}</h1><p>{result[1]}</p>'

if __name__ == '__main__':
    app.run()
