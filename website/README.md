# PUKAAR public website

Static site for **https://pukaaralert.com** — Privacy Policy, Terms, Delete Account, and company legal pages.

**Legal entity:** AXISPOINT INNOVATIONS PRIVATE LIMITED (GSTIN 09ABECA8628G1ZG)  
**Contact:** contact@pukaaralert.com

## Public URLs (no `.html`)

- `/` — Home  
- `/privacy-policy`  
- `/terms`  
- `/delete-account`  
- `/about`  
- `/contact`  
- `/data-safety`  

## Deploy

```bash
rsync -avz --delete --exclude README.md website/ root@SERVER:/var/www/pukaaralert/
nginx -t && systemctl reload nginx
```
