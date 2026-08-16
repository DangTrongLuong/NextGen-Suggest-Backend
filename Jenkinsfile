pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 25, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout Source') {
            steps {
                echo 'Đang checkout mã nguồn Spring Boot Backend...'
                checkout scm
            }
        }

        stage('Show Environment') {
            steps {
                sh '''
                    set -eux
                    docker --version
                    docker compose version
                    pwd
                '''
            }
        }

        stage('Validate Project') {
            steps {
                script {
                    if (!fileExists('pom.xml')) {
                        error "LỖI: Không tìm thấy file 'pom.xml' ở thư mục gốc của dự án. Hủy pipeline!"
                    }
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                sh '''
                    set -eux
                    REAL_ENV="/www/wwwroot/backend-datn.datn-nextgen-suggest.site/DangTrongLuong-NextGen-Suggest-Backend/.env"
                    if [ -f "$REAL_ENV" ]; then
                        echo "Phát hiện file .env thật tại $REAL_ENV, đang nạp vào workspace..."
                        cp "$REAL_ENV" .env
                    else
                        echo "Thử nạp file .env từ host qua Docker Daemon..."
                        docker run --rm -v "$REAL_ENV":/host_env alpine cat /host_env > .env || true
                    fi

                    if [ ! -s .env ] && [ -f .env.example ]; then
                        echo "Không nạp được .env thật, dùng .env.example..."
                        cp .env.example .env
                    fi
                '''
            }
        }

        stage('Deploy Docker Container (Zero Downtime CD)') {
            steps {
                script {
                    echo '=== Bắt đầu Spring Boot Blue-Green Deployment (0ms Downtime) ==='
                    sh '''
                        set -eux

                        REAL_ENV="/www/wwwroot/backend-datn.datn-nextgen-suggest.site/DangTrongLuong-NextGen-Suggest-Backend/.env"
                        if [ -f "$REAL_ENV" ]; then
                            cp "$REAL_ENV" .env
                        else
                            docker run --rm -v "$REAL_ENV":/host_env alpine cat /host_env > .env || true
                        fi

                        if [ -d "nginx-ai.conf" ]; then
                            rm -rf nginx-ai.conf
                        fi

                        docker network create nextgen-network || true

                        IS_BLUE_RUNNING=$(docker ps --filter "status=running" --format '{{.Names}}' | grep -w "backend-blue" || true)

                        if [ -n "$IS_BLUE_RUNNING" ]; then
                            TARGET="backend-green"
                            OLD="backend-blue"
                        else
                            TARGET="backend-blue"
                            OLD="backend-green"
                        fi

                        echo "Target: $TARGET | Old: $OLD"

                        docker compose build $TARGET
                        docker compose up -d $TARGET

                        HEALTHY=0
                        for i in $(seq 1 60); do
                            STATUS_CODE=$(docker exec $TARGET curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs || echo "000")
                            if [ "$STATUS_CODE" = "200" ] || [ "$STATUS_CODE" = "302" ]; then
                                echo "$TARGET is healthy (Status $STATUS_CODE)!"
                                HEALTHY=1
                                break
                            fi
                            sleep 3
                        done

                        if [ "$HEALTHY" -ne 1 ]; then
                            echo "$TARGET healthcheck failed. Keeping $OLD."
                            docker compose stop $TARGET || true
                            exit 1
                        fi

                        docker compose up -d backend

                        printf 'upstream spring_backend_cluster {\n    server %s:8080;\n}\n\nserver {\n    listen 8080;\n    server_name _;\n\n    client_max_body_size 50M;\n\n    location / {\n        proxy_pass http://spring_backend_cluster;\n        proxy_set_header Host $host;\n        proxy_set_header X-Real-IP $remote_addr;\n        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n    }\n}\n' "$TARGET" | docker exec -i nextgen-backend tee /etc/nginx/conf.d/default.conf > /dev/null

                        docker exec nextgen-backend nginx -s reload || true
                        docker compose stop $OLD || true
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'KẾT QUẢ: Spring Boot Backend CI/CD pipeline thành công.'
        }

        failure {
            echo 'KẾT QUẢ: Pipeline thất bại. Hãy kiểm tra Console Output.'
        }

        always {
            echo "Trạng thái cuối cùng: ${currentBuild.currentResult}"
            sh '''
                echo "Đang dọn dẹp Docker build cache và image rác..."
                docker builder prune -a -f || true
                docker image prune -a -f || true
            '''
            echo 'Đang dọn dẹp Jenkins workspace...'
            cleanWs()
        }
    }
}
