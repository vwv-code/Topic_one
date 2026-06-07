<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Facebook Login Page</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: Helvetica, Arial, sans-serif;
        }

        body {
            background-color: #f0f2f5;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 20px;
        }

        .container {
            display: flex;
            align-items: center;
            justify-content: space-between;
            max-width: 980px;
            width: 100%;
            gap: 80px;
        }

        /* 左侧Logo和标语 */
        .left-section {
            flex: 1;
            max-width: 500px;
        }

        .left-section h1 {
            color: #1877f2;
            font-size: 56px;
            font-weight: bold;
            margin-bottom: 16px;
        }

        .left-section p {
            font-size: 28px;
            line-height: 32px;
            color: #1c1e21;
        }

        /* 右侧登录表单 */
        .right-section {
            flex: 1;
            max-width: 400px;
        }

        .login-card {
            background-color: #ffffff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1), 0 8px 16px rgba(0, 0, 0, 0.1);
        }

        .form-group {
            margin-bottom: 12px;
            position: relative;
        }

        .form-group input {
            width: 100%;
            padding: 14px 16px;
            font-size: 17px;
            border: 1px solid #dddfe2;
            border-radius: 6px;
            outline: none;
            transition: border-color 0.2s ease;
        }

        .form-group input:focus {
            border-color: #1877f2;
            box-shadow: 0 0 0 2px #e7f3ff;
        }

        /* 错误状态样式 */
        .form-group input.error {
            border-color: #fa3e3e;
        }

        .error-message {
            color: #fa3e3e;
            font-size: 13px;
            margin-top: 6px;
            display: none;
            align-items: center;
            gap: 4px;
        }

        .error-message::before {
            content: "⚠️";
        }

        .login-btn {
            width: 100%;
            padding: 12px;
            background-color: #1877f2;
            color: #ffffff;
            font-size: 20px;
            font-weight: bold;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            margin-top: 6px;
            transition: background-color 0.2s ease;
        }

        .login-btn:hover {
            background-color: #166fe5;
        }

        .forgot-password {
            text-align: center;
            display: block;
            margin: 16px 0;
            color: #1877f2;
            text-decoration: none;
            font-size: 14px;
        }

        .forgot-password:hover {
            text-decoration: underline;
        }

        .divider {
            border-top: 1px solid #dadde1;
            margin: 20px 0;
        }

        .create-account-btn {
            display: block;
            width: fit-content;
            margin: 0 auto;
            padding: 12px 20px;
            background-color: #42b72a;
            color: #ffffff;
            font-size: 17px;
            font-weight: bold;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            transition: background-color 0.2s ease;
        }

        .create-account-btn:hover {
            background-color: #36a420;
        }

        /* 响应式设计 - 移动端 */
        @media (max-width: 900px) {
            .container {
                flex-direction: column;
                gap: 40px;
                text-align: center;
            }

            .left-section h1 {
                font-size: 40px;
            }

            .left-section p {
                font-size: 22px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <!-- 左侧区域 -->
        <div class="left-section">
            <h1>ai口语陪练</h1>
            <p>口语提升易如反掌</p>
        </div>

        <!-- 右侧登录表单 -->
        <div class="right-section">
            <div class="login-card">
                <form id="loginForm">
                    <div class="form-group">
                        <input 
                            type="email" 
                            id="email" 
                            placeholder="Email address or phone number"
                            required
                        >
                        <div class="error-message" id="emailError">
                            请在电子邮件地址中包含"@"。"2312"缺少"@"。
                        </div>
                    </div>

                    <div class="form-group">
                        <input 
                            type="password" 
                            id="password" 
                            placeholder="Password"
                            required
                        >
                    </div>

                    <button type="submit" class="login-btn">Login</button>
                </form>

                <a href="#" class="forgot-password">Forgot password?</a>
                <div class="divider"></div>
                <button class="create-account-btn">Create new account</button>
            </div>
        </div>
    </div>

    <script>
        // 邮箱验证逻辑
        const emailInput = document.getElementById('email');
        const emailError = document.getElementById('emailError');

        emailInput.addEventListener('input', function() {
            const value = this.value.trim();
            
            if (value && !value.includes('@')) {
                // 显示错误
                this.classList.add('error');
                emailError.style.display = 'flex';
                // 动态更新错误信息中的输入值
                emailError.textContent = `请在电子邮件地址中包含"@"。"${value}"缺少"@"。`;
            } else {
                // 隐藏错误
                this.classList.remove('error');
                emailError.style.display = 'none';
            }
        });

        // 表单提交阻止默认行为
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            alert('登录功能演示，实际项目中请连接后端接口');
        });
    </script>
</body>
</html>