var superagent = require('superagent');

describe('Homepage Get', function(){
  it('Homepage should respond to GET',function(){
    superagent
      .get('/')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('Sign Up Page Get', function(){
  it('Register Page respond to GET',function(){
    superagent
      .get('/signUp')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('Login Page Get', function(){
  it('Login Page respond to GET',function(){
    superagent
      .get('/login')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('Profile Page Get', function(){
  it('Login Page respond to GET',function(){
    superagent
      .get('/profile')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('House Page Get', function(){
  it('House Page respond to GET',function(){
    superagent
      .get('/House')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('Service Page Get', function(){
  it('Service Page respond to GET',function(){
    superagent
      .get('/service')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('Logout Page Get', function(){
  it('Logout Page respond to GET',function(){
    superagent
      .get('/logout')
      .end(function(res){
        expect(res.status).to.equal(200);
    })
  })
});

describe('Login with invalid account', function(){
  it('Login Page respond to POST',function(){
    var params = {
            username: "The Lord of the Rings",
            password: "J.R.R. Tolkien",
        }
        
    superagent
      .post('http://localhost:3000/login')
      .send(params)
      .end(function(res){
        console.log(res.body);
        expect(res.status).to.equal(200);
    })
  })
});

describe('Login with correct account', function(){
  it('Login Page respond to POST',function(){
    var params = {
            username: "pujari",
            password: "pujari",
        }
        
    superagent
      .post('http://localhost:3000/login')
      .send(params)
      .end(function(res){
        console.log(res.body);
        expect(res.status).to.equal(200);
    })
  })
});

describe('Access profile when not logged in with account', function(){
  it('Login Page respond to POST',function(){
    var params = {
            username: "The Lord of the Rings",
            password: "J.R.R. Tolkien",
        }
        
    superagent
      .post('http://localhost:3000/home')
      .send(params)
      .end(function(res){
        console.log(res.body);
        expect(res.status).to.equal(200);
    })
  })
});
