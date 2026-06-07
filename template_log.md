<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>新拟态登录界面</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdn.jsdelivr.net/npm/font-awesome@4.7.0/css/font-awesome.min.css" rel="stylesheet">
    <style type="text/tailwindcss">
        @layer utilities {
            .neu-card {
                background: #e0e5ec;
                box-shadow: 20px 20px 60px #bec3c9, -20px -20px 60px #ffffff;
            }
            .neu-input {
                background: #e0e5ec;
                box-shadow: inset 6px 6px 12px #bec3c9, inset -6px -6px 12px #ffffff;
            }
            .neu-btn {
                background: #e0e5ec;
                box-shadow: 6px 6px 12px #bec3c9, -6px -6px 12px #ffffff;
                transition: all 0.3s ease;
            }
            .neu-btn:active {
                box-shadow: inset 6px 6px 12px #bec3c9, inset -6px -6px 12px #ffffff;
            }
            .neu-social {
                background: #e0e5ec;
                box-shadow: 4px 4px 8px #bec3c9, -4px -4px 8px #ffffff;
                transition: all 0.3s ease;
            }
            .neu-social:active {
                box-shadow: inset 4px 4px 8px #bec3c9, inset -4px -4px 8px #ffffff;
            }
        }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center bg-[#e0e5ec] font-sans">
    <div class="w-full max-w-md mx-4">
        <div class="neu-card rounded-3xl p-8">
            <!-- 头部 -->
            <div class="text-center mb-8">
                <div class="w-20 h-20 neu-card rounded-full flex items-center justify-center mx-auto mb-4">
                    <i class="fa fa-user-circle text-4xl text-gray-600"></i>
                </div>
                <h1 class="text-2xl font-bold text-gray-700 mb-2">欢迎回来</h1>
                <p class="text-gray-500">请登录您的账户</p>
            </div>
            
            <!-- 表单 -->
            <form class="space-y-6">
                <div class="relative">
                    <input type="email" id="email" class="w-full px-5 py-4 rounded-xl neu-input text-gray-700 placeholder-transparent focus:outline-none" placeholder="邮箱地址" required>
                    <label for="email" class="absolute left-5 top-4 text-gray-500 transition-all duration-300 pointer-events-none">邮箱地址</label>
                </div>
                
                <div class="relative">
                    <input type="password" id="password" class="w-full px-5 py-4 rounded-xl neu-input text-gray-700 placeholder-transparent focus:outline-none" placeholder="密码" required>
                    <label for="password" class="absolute left-5 top-4 text-gray-500 transition-all duration-300 pointer-events-none">密码</label>
                    <button type="button" class="absolute right-5 top-4 text-gray-500 hover:text-gray-700 transition-colors">
                        <i class="fa fa-eye-slash"></i>
                    </button>
                </div>
                
                <div class="flex items-center justify-between">
                    <label class="flex items-center text-gray-600 text-sm">
                        <input type="checkbox" class="mr-2 rounded border-gray-300 bg-[#e0e5ec] text-indigo-500 focus:ring-indigo-500">
                        记住我
                    </label>
                    <a href="#" class="text-gray-600 text-sm hover:text-indigo-500 transition-colors">忘记密码？</a>
                </div>
                
                <button type="submit" class="w-full py-4 rounded-xl neu-btn text-gray-700 font-medium hover:text-indigo-500">
                    登录
                </button>
                
                <!-- 分割线 -->
                <div class="flex items-center my-6">
                    <div class="flex-1 h-px bg-gray-300"></div>
                    <span class="px-4 text-gray-500 text-sm">或者使用</span>
                    <div class="flex-1 h-px bg-gray-300"></div>
                </div>
                
                <!-- 社交登录 -->
                <div class="grid grid-cols-3 gap-4">
                    <button type="button" class="py-3 rounded-xl neu-social text-gray-600 hover:text-red-500">
                        <i class="fa fa-google"></i>
                    </button>
                    <button type="button" class="py-3 rounded-xl neu-social text-gray-600 hover:text-gray-800">
                        <i class="fa fa-github"></i>
                    </button>
                    <button type="button" class="py-3 rounded-xl neu-social text-gray-600 hover:text-green-500">
                        <i class="fa fa-weixin"></i>
                    </button>
                </div>
            </form>
            
            <!-- 底部 -->
            <div class="text-center mt-8">
                <p class="text-gray-500 text-sm">还没有账户？ <a href="#" class="text-indigo-500 font-medium">立即注册</a></p>
            </div>
        </div>
    </div>

    <script>
        // 浮动标签效果
        document.querySelectorAll('input').forEach(input => {
            if (input.value.trim() !== '') {
                input.nextElementSibling.classList.add('text-xs', '-translate-y-2.5', 'text-indigo-500');
            }
            
            input.addEventListener('focus', () => {
                input.nextElementSibling.classList.add('text-xs', '-translate-y-2.5', 'text-indigo-500');
            });
            
            input.addEventListener('blur', () => {
                if (input.value.trim() === '') {
                    input.nextElementSibling.classList.remove('text-xs', '-translate-y-2.5', 'text-indigo-500');
                }
            });
        });
        
        // 密码显示切换
        document.querySelectorAll('.fa-eye-slash, .fa-eye').forEach(icon => {
            icon.addEventListener('click', () => {
                const input = icon.parentElement.previousElementSibling;
                input.type = input.type === 'password' ? 'text' : 'password';
                icon.classList.toggle('fa-eye-slash');
                icon.classList.toggle('fa-eye');
            });
        });
        
        // 表单提交
        document.querySelector('form').addEventListener('submit', (e) => {
            e.preventDefault();
            const btn = e.target.querySelector('button[type="submit"]');
            const originalText = btn.textContent;
            btn.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i> 登录中...';
            btn.disabled = true;
            
            setTimeout(() => {
                btn.textContent = originalText;
                btn.disabled = false;
                alert('登录成功！');
            }, 1500);
        });
    </script>
</body>
</html>